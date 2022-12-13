/*
 * Copyright 2022 Netflix, Inc.
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

package com.netflix.kayenta.azure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.azure.storage.BlobsStorageService;
import com.netflix.kayenta.index.CanaryConfigIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty("kayenta.blobs.enabled")
@ComponentScan({"com.netflix.kayenta.blobs"})
@Slf4j
public class BlobsConfiguration {

  @Bean
  public BlobsStorageService blobsStorageService(
      CanaryConfigIndex canaryConfigIndex, ObjectMapper kayentaObjectMapper) {
    return new BlobsStorageService(kayentaObjectMapper, canaryConfigIndex);
  }
}
