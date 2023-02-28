/*
 * Copyright 2023 Netflix, Inc.
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

package com.netflix.kayenta.sql.accounts;

import static junit.framework.TestCase.assertEquals;

import com.netflix.kayenta.sql.MockAccountCredentials;
import java.util.List;
import javax.annotation.PostConstruct;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "sql.enabled=true")
public class IntegrationTestDataTest {

  @Autowired SqlBackedAccountCredentialsRepository repository;
  MockAccountCredentials accountCredentials;

  @PostConstruct
  public void verifyCanReadBasicDataAndWriteIt() {
    accountCredentials =
        MockAccountCredentials.builder()
            .name("test-account")
            .locations(List.of("someplace"))
            .sensitiveData("doNOTUseInAPI")
            .nonSensitiveData("shouldbeInApi")
            .build();
    repository.save(accountCredentials);
  }

  @Test
  public void verifyCanReadRepo() {
    MockAccountCredentials creds =
        (MockAccountCredentials) repository.findById("test-account").orElseThrow();
    // When writing to the database, make SURE we have ALL THe data persisted.
    assertEquals(creds, accountCredentials);
  }
}
