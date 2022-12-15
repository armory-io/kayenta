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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.graphite.service.GraphiteRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.squareup.okhttp.OkHttpClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import retrofit.converter.JacksonConverter;

@Configuration
@ConditionalOnProperty("kayenta.graphite.enabled")
@ComponentScan({"com.netflix.kayenta.graphite"})
@Slf4j
public class GraphiteConfiguration {
  @Bean
  @ConfigurationProperties("kayenta.graphite")
  @RefreshScope
  GraphiteConfigurationProperties graphiteConfigurationProperties() {
    return new GraphiteConfigurationProperties();
  }

  @Bean
  @ConfigurationProperties("kayenta.graphite.test-controller-defaults")
  GraphiteConfigurationTestControllerDefaultProperties
      graphiteConfigurationTestControllerDefaultProperties() {
    return new GraphiteConfigurationTestControllerDefaultProperties();
  }

  @Bean
  boolean graphiteAccountConfig(
      GraphiteConfigurationProperties graphiteConfigurationProperties,
      RetrofitClientFactory retrofitClientFactory,
      ObjectMapper objectMapper,
      OkHttpClient okHttpClient,
      AccountCredentialsRepository accountCredentialsRepository) {

    for (GraphiteManagedAccount account : graphiteConfigurationProperties.getAccounts()) {
      List<AccountCredentials.Type> supportedTypes = account.getSupportedTypes();

      if (!CollectionUtils.isEmpty(supportedTypes)) {
        if (supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
          account.setGraphiteRemoteService(
              retrofitClientFactory.createClient(
                  GraphiteRemoteService.class,
                  new JacksonConverter(objectMapper),
                  account.getBaseUrl(),
                  okHttpClient));
        }
      }

      accountCredentialsRepository.save(account);
    }

    log.info(
        "Populated GraphiteMetricsService with {} Graphite accounts.",
        graphiteConfigurationProperties.getAccounts().size());
    return true;
  }
}
