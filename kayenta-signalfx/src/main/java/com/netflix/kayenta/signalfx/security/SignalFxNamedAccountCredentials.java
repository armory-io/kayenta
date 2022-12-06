/*
 * Copyright (c) 2018 Nike, inc.
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
 *
 */

package com.netflix.kayenta.signalfx.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.signalfx.metrics.SignalFxMetricsService;
import com.netflix.kayenta.signalfx.service.SignalFxSignalFlowRemoteService;
import java.util.List;
import javax.validation.constraints.NotNull;

import com.netflix.kayenta.storage.StorageService;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class SignalFxNamedAccountCredentials extends AccountCredentials<SignalFxNamedAccountCredentials> {

  @NotNull private String name;

  @NotNull @Singular private List<Type> supportedTypes;

  @NotNull private SignalFxCredentials credentials;

  @NotNull private RemoteService endpoint;
  private MetricsService<SignalFxNamedAccountCredentials> metricsService;

  @Override
  public String getType() {
    return "signalfx";
  }

  @Override
  public MetricsService<SignalFxNamedAccountCredentials> getMetricsService() {
    return metricsService;
  }

  @Override
  public StorageService<SignalFxNamedAccountCredentials> getStorageService() {
    return null;
  }

  @JsonIgnore SignalFxSignalFlowRemoteService signalFlowService;
}
