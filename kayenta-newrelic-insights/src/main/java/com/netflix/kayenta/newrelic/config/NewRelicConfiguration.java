/*
 * Copyright 2018 Adobe
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

package com.netflix.kayenta.newrelic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.newrelic.metrics.NewRelicMetricsService;
import com.netflix.kayenta.newrelic.metrics.NewRelicQueryBuilderService;
import com.netflix.kayenta.newrelic.service.NewRelicRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.squareup.okhttp.OkHttpClient;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import retrofit.converter.JacksonConverter;

@Configuration
@ConditionalOnProperty("kayenta.newrelic.enabled")
@ComponentScan({"com.netflix.kayenta.newrelic"})
@Slf4j
public class NewRelicConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.newrelic")
  NewRelicConfigurationProperties newrelicConfigurationProperties() {
    return new NewRelicConfigurationProperties();
  }

  @Bean
  @ConfigurationProperties("kayenta.newrelic.test-controller-defaults")
  NewRelicConfigurationTestControllerDefaultProperties
      newrelicConfigurationTestControllerDefaultProperties() {
    return new NewRelicConfigurationTestControllerDefaultProperties();
  }

  /**
   * Create a map of account name to scope configurations. This allows operators to define default
   * scope and location keys which can sometimes be the same for all users within an org.
   *
   * <p>For example, some companies always add the attribute region to be the current deployed
   * region, so the defaultLocation key can be `region`
   *
   * @param newrelicConfigurationProperties Wrapper object around the list of configured accounts.
   * @return map of account name to default scope configurations.
   */
  @Bean
  Map<String, NewRelicScopeConfiguration> newrelicScopeConfigurationMap(
      NewRelicConfigurationProperties newrelicConfigurationProperties) {
    return newrelicConfigurationProperties.getAccounts().stream()
        .collect(
            Collectors.toMap(
                NewRelicManagedAccount::getName,
                accountConfig ->
                    NewRelicScopeConfiguration.builder()
                        .defaultScopeKey(accountConfig.getDefaultScopeKey())
                        .defaultLocationKey(accountConfig.getDefaultLocationKey())
                        .build()));
  }

  @PostConstruct
  public void initializeAccounts(
      RetrofitClientFactory retrofitClientFactory,
      ObjectMapper objectMapper,
      OkHttpClient okHttpClient,
      AccountCredentialsRepository accountCredentialsRepository,
      NewRelicConfigurationProperties newRelicConfigurationProperties) {

    for (NewRelicManagedAccount account : newRelicConfigurationProperties.getAccounts()) {

      if (account.getBaseUrl() == null) {
        account.setBaseUrl("https://insights-api.newrelic.com");
      }

      if (!CollectionUtils.isEmpty(account.getSupportedTypes())
          && account.getSupportedTypes().contains(AccountCredentials.Type.METRICS_STORE)) {
        account.setNewRelicRemoteService(
            retrofitClientFactory.createClient(
                NewRelicRemoteService.class,
                new JacksonConverter(objectMapper),
                account.getBaseUrl(),
                okHttpClient));
      }

      accountCredentialsRepository.save(account);
    }
  }

  @Bean
  MetricsService newrelicMetricsService(
      Map<String, NewRelicScopeConfiguration> newrelicScopeConfigurationMap,
      NewRelicConfigurationProperties newrelicConfigurationProperties) {

    log.info(
        "Configured the New Relic Metrics Service with the following accounts: {}",
        newrelicConfigurationProperties.getAccounts().stream()
            .map(NewRelicManagedAccount::getName)
            .collect(Collectors.joining(",")));

    return new NewRelicMetricsService(
        newrelicScopeConfigurationMap, new NewRelicQueryBuilderService());
  }
}
