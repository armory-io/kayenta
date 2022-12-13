/*
 * Copyright 2020 Playtika, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.netflix.kayenta.service;

import com.netflix.kayenta.metrics.MetricSetPair;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MetricSetPairListService {

  @Autowired private final AccountCredentialsRepository accountCredentialsRepository;
  @Autowired private final StorageServiceRepository storageServices;

  public List<MetricSetPair> loadMetricSetPairList(String account, String metricSetPairListId) {
    AccountCredentials accountCredentials = accountCredentialsRepository.getRequiredOne(account);
    return (List<MetricSetPair>)
        storageServices
            .getRequiredOne(accountCredentials)
            .loadObject(accountCredentials, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
  }

  public Optional<MetricSetPair> loadMetricSetPair(
      String account, String metricSetPairListId, String metricSetPairId) {
    AccountCredentials accountCredentials = accountCredentialsRepository.getRequiredOne(account);
    StorageService storageService = storageServices.getRequiredOne(accountCredentials);

    List<MetricSetPair> metricSetPairList =
        (List<MetricSetPair>)
            storageService.loadObject(
                accountCredentials, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
    return metricSetPairList.stream()
        .filter(metricSetPair -> metricSetPair.getId().equals(metricSetPairId))
        .findFirst();
  }

  public String storeMetricSetPairList(String account, List<MetricSetPair> metricSetPairList) {
    AccountCredentials accountCredentials = accountCredentialsRepository.getRequiredOne(account);
    StorageService storageService = storageServices.getRequiredOne(accountCredentials);
    String metricSetPairListId = UUID.randomUUID() + "";

    storageService.storeObject(
        accountCredentials,
        ObjectType.METRIC_SET_PAIR_LIST,
        metricSetPairListId,
        metricSetPairList);
    return metricSetPairListId;
  }

  public void deleteMetricSetPairList(String account, String metricSetPairListId) {
    AccountCredentials accountCredentials = accountCredentialsRepository.getRequiredOne(account);

    storageServices
        .getRequiredOne(accountCredentials)
        .deleteObject(accountCredentials, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
  }

  public List<Map<String, Object>> listAllMetricSetPairLists(String account) {

    AccountCredentials accountCredentials = accountCredentialsRepository.getRequiredOne(account);
    return storageServices
        .getRequiredOne(accountCredentials)
        .listObjectKeys(accountCredentials, ObjectType.METRIC_SET_PAIR_LIST);
  }
}
