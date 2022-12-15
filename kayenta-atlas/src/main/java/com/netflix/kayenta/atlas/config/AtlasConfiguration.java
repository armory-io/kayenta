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

package com.netflix.kayenta.atlas.config;

import com.netflix.kayenta.atlas.backends.AtlasStorageUpdater;
import com.netflix.kayenta.atlas.backends.AtlasStorageUpdaterService;
import com.netflix.kayenta.atlas.backends.BackendUpdater;
import com.netflix.kayenta.atlas.backends.BackendUpdaterService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("kayenta.atlas.enabled")
@ComponentScan({"com.netflix.kayenta.atlas"})
@Slf4j
public class AtlasConfiguration {

  @Bean
  @ConfigurationProperties("kayenta.atlas")
  @RefreshScope
  AtlasConfigurationProperties atlasConfigurationProperties() {
    return new AtlasConfigurationProperties();
  }

  @Bean
  public boolean configureAtlasAccounts(
      AccountCredentialsRepository accountCredentialsRepository,
      AtlasConfigurationProperties accountConfig,
      AtlasStorageUpdaterService atlasStorageUpdaterService,
      BackendUpdaterService backendUpdaterService) {
    for (AtlasManagedAccount atlasManagedAccount : accountConfig.getAccounts()) {
      log.info(
          "Configuring account {} for type of {}",
          atlasManagedAccount.getName(),
          atlasManagedAccount.getSupportedTypes());
      BackendUpdater backendUpdater =
          BackendUpdater.builder().uri(atlasManagedAccount.getBackendsJsonBaseUrl()).build();
      AtlasStorageUpdater atlasStorageUpdater =
          AtlasStorageUpdater.builder().uri(atlasManagedAccount.getBackendsJsonBaseUrl()).build();
      atlasManagedAccount.setBackendUpdater(backendUpdater);
      atlasManagedAccount.setAtlasStorageUpdater(atlasStorageUpdater);
      backendUpdaterService.add(backendUpdater);
      atlasStorageUpdaterService.add(atlasStorageUpdater);
      accountCredentialsRepository.save(atlasManagedAccount);
    }
    return true;
  }
}
