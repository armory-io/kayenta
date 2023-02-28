/*
 * Copyright 2020 Playtika.
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

package com.netflix.kayenta.security;

import static com.netflix.kayenta.security.AccountCredentials.Type.CONFIGURATION_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.METRICS_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.OBJECT_STORE;
import static com.netflix.kayenta.security.AccountCredentials.Type.REMOTE_JUDGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netflix.kayenta.MockAccountCredentials;
import org.junit.Test;

public class MapBackedAccountCredentialsRepositoryTest {

  AccountCredentialsRepository repository = new MapBackedAccountCredentialsRepository();

  @Test
  public void getOne_returnsPresentAccount() {
    AccountCredentials account = new MockAccountCredentials("account1");
    repository.save(account);
    assertThat(repository.findById("account1")).hasValue(account);
  }

  @Test
  public void getRequiredOne_throwsExceptionIfAccountNotPresent() {
    assertThatThrownBy(() -> repository.getRequiredOne("account"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unable to resolve account account.");
  }

  @Test
  public void getRequiredOne_returnsPresentAccount() {
    AccountCredentials account = new MockAccountCredentials("account1");
    repository.save(account);

    AccountCredentials actual = repository.getRequiredOne("account1");
    assertThat(actual).isEqualTo(account);
  }

  @Test
  public void getAllAccountsOfType_returnsAccountsOfSpecificTypeOnly() {
    AccountCredentials account1 =
        new MockAccountCredentials("account1", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account2 =
        new MockAccountCredentials(
            "account2", METRICS_STORE, OBJECT_STORE, CONFIGURATION_STORE, REMOTE_JUDGE);
    AccountCredentials account3 = new MockAccountCredentials("account3");
    repository.save(account1);
    repository.save(account2);
    repository.save(account3);

    assertThat(repository.getAllOf(METRICS_STORE)).containsOnly(account1, account2);
    assertThat(repository.getAllOf(OBJECT_STORE)).containsOnly(account1, account2);
    assertThat(repository.getAllOf(CONFIGURATION_STORE)).containsOnly(account2);
    assertThat(repository.getAllOf(REMOTE_JUDGE)).containsOnly(account2);
  }

  @Test
  public void getRequiredOneBy_returnsActualAccountByName() {
    AccountCredentials account1 = new MockAccountCredentials("account1", METRICS_STORE);
    repository.save(account1);

    assertThat(repository.getAccountOrFirstOfTypeWhenEmptyAccount("account1", METRICS_STORE))
        .isEqualTo(account1);
  }

  @Test
  public void getRequiredOneBy_returnsFirstAvailableAccountByTypeIfNameIsNotProvided() {
    AccountCredentials account1 =
        new MockAccountCredentials("account1", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account2 =
        new MockAccountCredentials("account2", METRICS_STORE, OBJECT_STORE);
    AccountCredentials account3 =
        new MockAccountCredentials("account3", METRICS_STORE, OBJECT_STORE);
    repository.save(account1);
    repository.save(account2);
    repository.save(account3);

    assertThat(repository.getAccountOrFirstOfTypeWhenEmptyAccount(null, METRICS_STORE))
        .isIn(account1, account2, account3);
    assertThat(repository.getAccountOrFirstOfTypeWhenEmptyAccount("", METRICS_STORE))
        .isIn(account1, account2, account3);
  }
}
