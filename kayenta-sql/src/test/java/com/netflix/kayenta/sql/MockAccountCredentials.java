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

package com.netflix.kayenta.sql;

import com.netflix.kayenta.security.AccountCredentials;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class MockAccountCredentials extends AccountCredentials<MockAccountCredentials> {
  private String sensitiveData;
  private String nonSensitiveData;

  public MockAccountCredentials(String s, Type... supportedTypes) {
    setSupportedTypes(List.of(supportedTypes));
    setName(s);
  }

  @Override
  public String getType() {
    return "test-creds";
  }
}
