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

package com.netflix.kayenta.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.retrofit.config.RemoteService;
import com.netflix.kayenta.storage.StorageService;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;


@Getter
@Setter
public abstract class AccountCredentials<T extends AccountCredentials> {
  private String name;
  //atlas/gcs/newrelic/etc.
  public abstract String getType();

  //I'd ARGUE this should be on an extension that's KaynetAccountcredentials instead.
  public abstract List<Type> getSupportedTypes();


  public enum Type {
    METRICS_STORE,
    OBJECT_STORE,
    CONFIGURATION_STORE,
    REMOTE_JUDGE;
  }

  @JsonIgnore
  public abstract <T> T getCredentials();
  public abstract MetricsService<T> getMetricsService();
  public abstract StorageService<T> getStorageService();

  public List<String> getLocations() {
    return Collections.emptyList();
  }

  /*
   * If this account provides a recommended list of locations, this can also be used by the UI to limit
   * the initially presented list to something shorter than "everything."  Note that this list may be
   * present even if locations() returns an empty list; this would imply that there are commonly
   * used locations, but the full list is unknown by the metrics service.
   */
  public List<String> getRecommendedLocations() {
    return Collections.emptyList();
  }


}
