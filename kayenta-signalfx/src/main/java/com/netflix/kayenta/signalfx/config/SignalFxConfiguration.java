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

package com.netflix.kayenta.signalfx.config;

import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.signalfx.service.SignalFxConverter;
import com.netflix.kayenta.signalfx.service.SignalFxSignalFlowRemoteService;
import com.squareup.okhttp.OkHttpClient;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty("kayenta.signalfx.enabled")
@ComponentScan({"com.netflix.kayenta.signalfx"})
@Slf4j
public class SignalFxConfiguration {

  private static final String SIGNAL_FX_SIGNAL_FLOW_ENDPOINT_URI = "https://stream.signalfx.com";

  @Bean
  @ConfigurationProperties("kayenta.signalfx")
  SignalFxConfigurationProperties signalFxConfigurationProperties() {
    return new SignalFxConfigurationProperties();
  }

  @Bean
  boolean signalFxMetricService(
      SignalFxConfigurationProperties signalFxConfigurationProperties,
      RetrofitClientFactory retrofitClientFactory,
      OkHttpClient okHttpClient,
      AccountCredentialsRepository accountCredentialsRepository) {

    for (SignalFxManagedAccount signalFxManagedAccount :
        signalFxConfigurationProperties.getAccounts()) {
      if (!StringUtils.hasText(signalFxManagedAccount.getBaseUrl())) {
        signalFxManagedAccount.setBaseUrl(SIGNAL_FX_SIGNAL_FLOW_ENDPOINT_URI);
      }
      signalFxManagedAccount.setScopeConfiguration(
          new SignalFxScopeConfiguration(
              signalFxManagedAccount.getDefaultScopeKey(),
              signalFxManagedAccount.getDefaultLocationKey()));

      if (!CollectionUtils.isEmpty(signalFxManagedAccount.getSupportedTypes())
          && signalFxManagedAccount
              .getSupportedTypes()
              .contains(AccountCredentials.Type.METRICS_STORE)) {
        signalFxManagedAccount.setSignalFlowService(
            retrofitClientFactory.createClient(
                SignalFxSignalFlowRemoteService.class,
                new SignalFxConverter(),
                signalFxManagedAccount.getBaseUrl(),
                okHttpClient));
      }

      accountCredentialsRepository.save(signalFxManagedAccount);
    }

    log.info(
        "Configured the SignalFx Metrics Service with the following accounts: {}",
        String.join(
            ",",
            signalFxConfigurationProperties.getAccounts().stream()
                .map(SignalFxManagedAccount::getName)
                .collect(Collectors.toList())));

    return true;
  }
}
