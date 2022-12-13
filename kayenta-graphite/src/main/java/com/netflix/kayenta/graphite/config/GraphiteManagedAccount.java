/*
 * Copyright 2018 Snap Inc.
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

package com.netflix.kayenta.graphite.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.canary.providers.metrics.GraphiteCanaryMetricSetQueryConfig;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class GraphiteManagedAccount extends AccountCredentials {
  @NotNull private String name;

  @NotNull @Getter @Setter private String baseUrl;
  @NotNull private RemoteService endpoint;

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void setEndpoint(RemoteService endpoint) {
    this.endpoint = endpoint;
    // NOMINALLY this is replaced by setBaseUrl in the future...
    this.baseUrl = endpoint.getBaseUrl();
  }

  @JsonIgnore private transient GraphiteRemoteService graphiteRemoteService;
  private List<AccountCredentials.Type> supportedTypes;

  @Override
  public String getType() {
    return GraphiteCanaryMetricSetQueryConfig.SERVICE_TYPE;
  }
}
