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

package com.netflix.kayenta.prometheus.config;

import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.prometheus.health.PrometheusHealthCache;
import com.netflix.kayenta.prometheus.health.PrometheusHealthIndicator;
import com.netflix.kayenta.prometheus.health.PrometheusHealthJob;
import com.netflix.kayenta.prometheus.metrics.PrometheusMetricsService;
import com.netflix.kayenta.prometheus.service.PrometheusRemoteService;
import com.netflix.kayenta.retrofit.config.RetrofitClientFactory;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.util.CollectionUtils;

@Configuration
@ConditionalOnProperty("kayenta.prometheus.enabled")
@ComponentScan({"com.netflix.kayenta.prometheus"})
@Slf4j
public class PrometheusConfiguration {

  @Bean
  MetricsService prometheusMetricsService(
      PrometheusResponseConverter prometheusConverter,
      PrometheusConfigurationProperties prometheusConfigurationProperties,
      RetrofitClientFactory retrofitClientFactory,
      OkHttpClient okHttpClient,
      AccountCredentialsRepository accountCredentialsRepository) {

    for (PrometheusManagedAccount prometheusManagedAccount :
        prometheusConfigurationProperties.getAccounts()) {
      String name = prometheusManagedAccount.getName();
      List<AccountCredentials.Type> supportedTypes = prometheusManagedAccount.getSupportedTypes();

      log.info("Registering Prometheus account {} with supported types {}.", name, supportedTypes);

      try {
        if (!CollectionUtils.isEmpty(supportedTypes)
            && supportedTypes.contains(AccountCredentials.Type.METRICS_STORE)) {
          PrometheusRemoteService prometheusRemoteService =
              retrofitClientFactory.createClient(
                  PrometheusRemoteService.class,
                  prometheusConverter,
                  prometheusManagedAccount.getEndpoint(),
                  okHttpClient,
                  prometheusManagedAccount.getUsername(),
                  prometheusManagedAccount.getPassword(),
                  prometheusManagedAccount.getUsernamePasswordFile(),
                  prometheusManagedAccount.getBearerToken());

          prometheusManagedAccount.setPrometheusRemoteService(prometheusRemoteService);
          accountCredentialsRepository.save(prometheusManagedAccount);
        }
      } catch (IOException e) {
        log.error("Problem registering Prometheus account {}:", name, e);
      }
    }
    ;

    return PrometheusMetricsService.builder()
        .scopeLabel(prometheusConfigurationProperties.getScopeLabel())
        .build();
  }

  @Conditional(PrometheusHealthEnabled.class)
  @Configuration(proxyBeanMethods = false)
  public static class PrometheusHealthConfiguration {

    @DependsOn("prometheusMetricsService")
    @Bean
    HealthIndicator prometheusHealthIndicator(PrometheusHealthCache prometheusHealthCache) {
      return new PrometheusHealthIndicator(prometheusHealthCache);
    }

    @DependsOn("prometheusMetricsService")
    @Bean
    PrometheusHealthJob prometheusHealthJob(
        PrometheusConfigurationProperties prometheusConfigurationProperties,
        AccountCredentialsRepository accountCredentialsRepository,
        PrometheusHealthCache prometheusHealthCache) {
      return new PrometheusHealthJob(
          prometheusConfigurationProperties, accountCredentialsRepository, prometheusHealthCache);
    }

    @Bean
    PrometheusHealthCache prometheusHealthCache() {
      return new PrometheusHealthCache();
    }
  }

  static class PrometheusHealthEnabled extends AllNestedConditions {
    PrometheusHealthEnabled() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(
        value = "kayenta.prometheus.health.enabled",
        havingValue = "true",
        matchIfMissing = false)
    static class enabledHealth {}

    @ConditionalOnProperty(value = "kayenta.prometheus.accounts[0].name", matchIfMissing = false)
    static class accountPresent {}
  }
}
