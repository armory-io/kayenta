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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetricSetPairListService {

  private final AccountCredentialsRepository accountCredentialsRepository;

  public List<MetricSetPair> loadMetricSetPairList(String accountName, String metricSetPairListId) {
    StorageService storageService = accountCredentialsRepository.<StorageService>getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE).getCredentials();

    return storageService.loadObject(accountName, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
  }

  public Optional<MetricSetPair> loadMetricSetPair(
      String accountName, String metricSetPairListId, String metricSetPairId) {
    StorageService storageService = accountCredentialsRepository.<StorageService>getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE).getCredentials();

    List<MetricSetPair> metricSetPairList =
        storageService.loadObject(accountName, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
    return metricSetPairList.stream()
        .filter(metricSetPair -> metricSetPair.getId().equals(metricSetPairId))
        .findFirst();
  }

  public String storeMetricSetPairList(String accountName, List<MetricSetPair> metricSetPairList) {
    StorageService storageService = accountCredentialsRepository.<StorageService>getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE).getCredentials();
    String metricSetPairListId = UUID.randomUUID() + "";

    storageService.storeObject(
            accountName,
        ObjectType.METRIC_SET_PAIR_LIST,
        metricSetPairListId,
        metricSetPairList);
    return metricSetPairListId;
  }

  public void deleteMetricSetPairList(String accountName, String metricSetPairListId) {
    StorageService storageService = accountCredentialsRepository.getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE).getCredentials();

    storageService.deleteObject(
            accountName, ObjectType.METRIC_SET_PAIR_LIST, metricSetPairListId);
  }

  public List<Map<String, Object>> listAllMetricSetPairLists(String accountName) {
    StorageService storageService = accountCredentialsRepository.getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE).getCredentials();

    return storageService.listObjectKeys(accountName, ObjectType.METRIC_SET_PAIR_LIST);
  }

  private String getAccount(String accountName) {
    return accountCredentialsRepository
        .getRequiredOneBy(accountName, AccountCredentials.Type.OBJECT_STORE)
        .getName();
  }
}
