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

package com.netflix.kayenta.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

public interface AccountCredentialsRepository extends CrudRepository<AccountCredentials, String> {

    default AccountCredentials getRequiredOneBy(String accountName, AccountCredentials.Type accountType) {
        return findById(accountName).filter(it -> it.getSupportedTypes().contains(accountType))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unable to resolve account of type " + accountType + "."));
    }

    default Set<AccountCredentials> getAllOf(AccountCredentials.Type credentialsType) {
        return StreamSupport.stream(findAll().spliterator(), true)
                .filter(accountCredentials -> accountCredentials.getSupportedTypes().contains(credentialsType))
                .collect(Collectors.toSet());
    }
}
