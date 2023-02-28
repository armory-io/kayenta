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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.repository.CrudRepository;

public interface AccountCredentialsRepository extends CrudRepository<AccountCredentials, String> {

  /*
  This does a "If the account is blank, lookup the first available of given type.  SHOULD ONLY be used for storage/config store type accounts
  IF account isn't blank, it's a wrapper to getRequiredOne which REQUIRES the account to exist or blows up
   */
  default AccountCredentials getAccountOrFirstOfTypeWhenEmptyAccount(
      String accountName, AccountCredentials.Type accountType) {
    if (StringUtils.isBlank(accountName)) {
      return getAllOf(accountType).stream().findFirst().orElseThrow();
    }
    return getRequiredOne(accountName);
  }

  default Set<AccountCredentials> getAllOf(AccountCredentials.Type credentialsType) {
    return StreamSupport.stream(findAll().spliterator(), true)
        .filter(
            accountCredentials -> accountCredentials.getSupportedTypes().contains(credentialsType))
        .collect(Collectors.toSet());
  }

  default AccountCredentials getRequiredOne(String accountName) {
    return findById(accountName)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("Unable to resolve account %s.", accountName)));
  }
}
