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

package com.netflix.kayenta.storage;

import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a basic wrapper around account credentials repo. It takes a list of registered service
 * beans, and figures out which storage/account services work for a given named account. IN THEORY
 * this is SOMEONE deprecated, as long term storage accounts could go away in favor of using SQL
 * storage service for ALL requests.
 */
@Component
public class StorageServiceRepository {
  List<StorageService> storageServiceList;
  AccountCredentialsRepository accountCredentialsRepository;

  StorageServiceRepository(
      @Autowired AccountCredentialsRepository accountCredentialsRepository,
      @Autowired List<StorageService> storageServiceList) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.storageServiceList = storageServiceList;
  }

  public Optional<StorageService> getOne(AccountCredentials account) {
    return storageServiceList.stream()
        .filter(storageService -> storageService.appliesTo(account))
        .findFirst();
  }

  public StorageService getRequiredOne(AccountCredentials account) {
    return getOne(account)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to resolve storage service " + account.getName() + "."));
  }

  public StorageService getRequiredOne(String accountName) {
    return getRequiredOne(accountCredentialsRepository.getRequiredOne(accountName));
  }
}
