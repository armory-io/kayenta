/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.controllers;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryConfigUpdateResponse;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canaryConfig")
@Slf4j
public class CanaryConfigController {

  private static final Pattern canaryConfigNamePattern = Pattern.compile("[A-Z,a-z,0-9,\\-,\\_]*");

  private final AccountCredentialsRepository accountCredentialsRepository;
  private final StorageServiceRepository storageServiceRepository;
  private final boolean disableMetricNameValidation;

  @Autowired
  public CanaryConfigController(
      AccountCredentialsRepository accountCredentialsRepository,
      StorageServiceRepository storageServiceRepository,
      @Value("${kayenta.disable.metricname.validation:false}")
          boolean disableMetricNameValidation) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.disableMetricNameValidation = disableMetricNameValidation;
    this.storageServiceRepository = storageServiceRepository;
  }

  @ApiOperation(value = "Retrieve a canary config from object storage")
  @RequestMapping(value = "/{canaryConfigId:.+}", method = RequestMethod.GET)
  public CanaryConfig loadCanaryConfig(
      @RequestParam(required = false) final String configurationAccountName,
      @PathVariable String canaryConfigId) {
    AccountCredentials credentials =
        accountCredentialsRepository.getAccountOrFirstOfTypeWhenEmptyAccount(
            configurationAccountName, AccountCredentials.Type.CONFIGURATION_STORE);
    StorageService configurationService = storageServiceRepository.getRequiredOne(credentials);
    return (CanaryConfig)
        configurationService.loadObject(credentials, ObjectType.CANARY_CONFIG, canaryConfigId);
  }

  @ApiOperation(value = "Write a canary config to object storage")
  @RequestMapping(consumes = "application/json", method = RequestMethod.POST)
  public CanaryConfigUpdateResponse storeCanaryConfig(
      @RequestParam(required = false) final String configurationAccountName,
      @RequestBody CanaryConfig canaryConfig)
      throws IOException {
    AccountCredentials accountCredentials =
        accountCredentialsRepository.getAccountOrFirstOfTypeWhenEmptyAccount(
            configurationAccountName, AccountCredentials.Type.CONFIGURATION_STORE);

    if (canaryConfig.getCreatedTimestamp() == null) {
      canaryConfig.setCreatedTimestamp(System.currentTimeMillis());
    }

    if (canaryConfig.getUpdatedTimestamp() == null) {
      canaryConfig.setUpdatedTimestamp(canaryConfig.getCreatedTimestamp());
    }

    canaryConfig.setCreatedTimestampIso(
        Instant.ofEpochMilli(canaryConfig.getCreatedTimestamp()).toString());
    canaryConfig.setUpdatedTimestampIso(
        Instant.ofEpochMilli(canaryConfig.getUpdatedTimestamp()).toString());

    if (StringUtils.hasText(canaryConfig.getId())) {
      // Ensure that the canary config id is stored within the canary config itself.
      canaryConfig = canaryConfig.toBuilder().id(UUID.randomUUID() + "").build();
    }

    String canaryConfigId = canaryConfig.getId();

    validateNameAndApplicationAttributes(canaryConfig);
    validateMetricConfigNames(canaryConfig);

    StorageService storageService = storageServiceRepository.getRequiredOne(accountCredentials);
    try {
      storageService.loadObject(accountCredentials, ObjectType.CANARY_CONFIG, canaryConfigId);
      throw new IllegalArgumentException("Canary config '" + canaryConfigId + "' already exists.");
    } catch (NotFoundException e) {
      storageService.storeObject(
          accountCredentials,
          ObjectType.CANARY_CONFIG,
          canaryConfigId,
          canaryConfig,
          canaryConfig.getName() + ".json",
          false);

      return CanaryConfigUpdateResponse.builder().canaryConfigId(canaryConfigId).build();
    }
  }

  @ApiOperation(value = "Update a canary config")
  @RequestMapping(
      value = "/{canaryConfigId:.+}",
      consumes = "application/json",
      method = RequestMethod.PUT)
  public CanaryConfigUpdateResponse updateCanaryConfig(
      @RequestParam(required = false) final String configurationAccountName,
      @PathVariable String canaryConfigId,
      @RequestBody CanaryConfig canaryConfig)
      throws IOException {
    AccountCredentials accountCredentials =
        accountCredentialsRepository.getAccountOrFirstOfTypeWhenEmptyAccount(
            configurationAccountName, AccountCredentials.Type.CONFIGURATION_STORE);
    StorageService storageService = storageServiceRepository.getRequiredOne(accountCredentials);

    canaryConfig.setUpdatedTimestamp(System.currentTimeMillis());
    canaryConfig.setUpdatedTimestampIso(
        Instant.ofEpochMilli(canaryConfig.getUpdatedTimestamp()).toString());

    validateNameAndApplicationAttributes(canaryConfig);
    validateMetricConfigNames(canaryConfig);

    try {
      storageService.loadObject(accountCredentials, ObjectType.CANARY_CONFIG, canaryConfigId);
    } catch (Exception e) {
      throw new IllegalArgumentException("Canary config '" + canaryConfigId + "' does not exist.");
    }

    // Ensure that the canary config id is stored within the canary config itself.
    if (StringUtils.isEmpty(canaryConfig.getId())) {
      canaryConfig = canaryConfig.toBuilder().id(canaryConfigId).build();
    }

    storageService.storeObject(
        accountCredentials,
        ObjectType.CANARY_CONFIG,
        canaryConfigId,
        canaryConfig,
        canaryConfig.getName() + ".json",
        true);

    return CanaryConfigUpdateResponse.builder().canaryConfigId(canaryConfigId).build();
  }

  private static void validateNameAndApplicationAttributes(@RequestBody CanaryConfig canaryConfig) {
    if (StringUtils.isEmpty(canaryConfig.getName())) {
      throw new IllegalArgumentException("Canary config must specify a name.");
    } else if (canaryConfig.getApplications() == null
        || canaryConfig.getApplications().size() == 0) {
      throw new IllegalArgumentException("Canary config must specify at least one application.");
    }

    String canaryConfigName = canaryConfig.getName();

    if (!canaryConfigNamePattern.matcher(canaryConfigName).matches()) {
      throw new IllegalArgumentException(
          "Canary config cannot be named '"
              + canaryConfigName
              + "'. Names must contain only letters, numbers, dashes (-) and underscores (_).");
    }
  }

  private void validateMetricConfigNames(CanaryConfig canaryConfig) {
    if (disableMetricNameValidation) {
      return;
    }

    List<CanaryMetricConfig> metrics = canaryConfig.getMetrics();

    if (CollectionUtils.isEmpty(metrics)) {
      return;
    }

    Set<String> metricNameSet = new HashSet<>();

    for (CanaryMetricConfig metricConfig : metrics) {
      String metricName = metricConfig.getName();

      if (StringUtils.isEmpty(metricName)) {
        throw new IllegalArgumentException("Metric config must specify a name.");
      } else if (metricNameSet.contains(metricName)) {
        throw new IllegalArgumentException(
            "Metric config name must be unique. '" + metricName + "' is duplicated.");
      } else {
        metricNameSet.add(metricName);
      }
    }
  }

  @ApiOperation(value = "Delete a canary config")
  @RequestMapping(value = "/{canaryConfigId:.+}", method = RequestMethod.DELETE)
  public void deleteCanaryConfig(
      @RequestParam(required = false) final String configurationAccountName,
      @PathVariable String canaryConfigId,
      HttpServletResponse response) {
    AccountCredentials accountCredentials =
        accountCredentialsRepository.getAccountOrFirstOfTypeWhenEmptyAccount(
            configurationAccountName, AccountCredentials.Type.CONFIGURATION_STORE);
    StorageService storageService = storageServiceRepository.getRequiredOne(accountCredentials);

    storageService.deleteObject(accountCredentials, ObjectType.CANARY_CONFIG, canaryConfigId);

    response.setStatus(HttpStatus.NO_CONTENT.value());
  }

  @ApiOperation(value = "Retrieve a list of canary config ids and timestamps")
  @RequestMapping(method = RequestMethod.GET)
  public List<Map<String, Object>> listAllCanaryConfigs(
      @RequestParam(required = false) final String configurationAccountName,
      @RequestParam(required = false, value = "application") final List<String> applications) {
    AccountCredentials accountCredentials =
        accountCredentialsRepository.getAccountOrFirstOfTypeWhenEmptyAccount(
            configurationAccountName, AccountCredentials.Type.CONFIGURATION_STORE);
    StorageService storageService = storageServiceRepository.getRequiredOne(accountCredentials);

    return storageService.listObjectKeys(
        accountCredentials, ObjectType.CANARY_CONFIG, applications, false);
  }
}
