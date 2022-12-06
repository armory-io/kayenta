/*
 * Copyright 2017 Netflix, Inc.
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

package com.netflix.kayenta.memory.security;

import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.storage.ObjectType;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Builder
@Data
public class MemoryNamedAccountCredentials extends AccountCredentials {

  @NotNull private String name;

  @NotNull @Singular private List<Type> supportedTypes;

  @NotNull private MemoryAccountCredentials credentials;

  @NotNull private Map<ObjectType, Map<String, Object>> objects;

  @NotNull private Map<ObjectType, Map<String, Map<String, Object>>> metadata;

  @Override
  public String getType() {
    return "memory";
  }
}
