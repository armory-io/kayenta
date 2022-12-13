/*
 * Copyright 2022 Netflix, Inc.
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

package com.netflix.kayenta.metrics;

import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a basic wrapper around account credentials repo. It takes a list of registered service
 * beans, and figures out which metrics/account services work for a given named account. IN THEORY
 * this is SOMEONE deprecated, as long term metrics accounts could go away in favor of using SQL
 * metrics service for ALL requests.
 */
@Component
public class MetricsServiceRepository {
  List<MetricsService> metricsServiceList;
  AccountCredentialsRepository accountCredentialsRepository;

  MetricsServiceRepository(
      @Autowired AccountCredentialsRepository accountCredentialsRepository,
      @Autowired List<MetricsService> metricsServiceList) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.metricsServiceList = metricsServiceList;
  }

  public Optional<MetricsService> getOne(AccountCredentials account) {
    return metricsServiceList.stream()
        .filter(metricsService -> metricsService.appliesTo(account))
        .findFirst();
  }

  public MetricsService getRequiredOne(String accountName) {
    return metricsServiceList.stream()
        .filter(
            metricsService ->
                metricsService.appliesTo(accountCredentialsRepository.getRequiredOne(accountName)))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to resolve metrics service " + accountName + "."));
  }

  public MetricsService getRequiredOne(AccountCredentials account) {
    return getOne(account)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to resolve metrics service " + account.getName() + "."));
  }
}
