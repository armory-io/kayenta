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

package com.netflix.kayenta.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import retrofit.RetrofitError;

@Component
@Slf4j
public class SynchronousQueryProcessor {
  private final Registry registry;
  private final MetricsRetryConfigurationProperties retryConfiguration;
  private final MetricsServiceRepository metricsRepo;
  private final StorageServiceRepository storageServiceRepo;

  @Autowired
  public SynchronousQueryProcessor(
      Registry registry,
      MetricsRetryConfigurationProperties retryConfiguration,
      MetricsServiceRepository metricsRepo,
      StorageServiceRepository storageServiceRepository) {
    this.registry = registry;
    this.retryConfiguration = retryConfiguration;
    this.metricsRepo = metricsRepo;
    this.storageServiceRepo = storageServiceRepository;
  }

  public String executeQuery(
      AccountCredentials metricsAccount,
      AccountCredentials storageAccount,
      CanaryConfig canaryConfig,
      int metricIndex,
      CanaryScope canaryScope)
      throws IOException {
    MetricsService metricsService = metricsRepo.getRequiredOne(metricsAccount);

    StorageService storageService = storageServiceRepo.getRequiredOne(storageAccount);

    Id queryId =
        registry
            .createId("canary.telemetry.query")
            .withTag("metricsStore", metricsService.getType());

    CanaryMetricConfig canaryMetricConfig = canaryConfig.getMetrics().get(metricIndex);
    List<MetricSet> metricSetList = null;

    // TODO: retry mechanism should be extracted to separate class
    int retries = 0;
    boolean success = false;

    while (!success) {
      try {
        registry.counter(queryId.withTag("retries", retries + "")).increment();
        metricSetList =
            metricsService.queryMetrics(
                metricsAccount, canaryConfig, canaryMetricConfig, canaryScope);
        success = true;
      } catch (RetrofitError e) {

        boolean retryable = isRetryable(e);
        if (retryable) {
          retries++;
          if (retries >= retryConfiguration.getAttempts()) {
            throw e;
          }
          long backoffPeriod = getBackoffPeriodMs(retries);
          try {
            Thread.sleep(backoffPeriod);
          } catch (InterruptedException ignored) {
          }
          Object error = e.getResponse() != null ? e.getResponse().getStatus() : e.getCause();
          log.warn(
              "Got {} result when querying for metrics. Retrying request (current attempt: "
                  + "{}, max attempts: {}, last backoff period: {}ms)",
              error,
              retries,
              retryConfiguration.getAttempts(),
              backoffPeriod);
        } else {
          throw e;
        }
      } catch (IOException | UncheckedIOException | RetryableQueryException e) {
        retries++;
        if (retries >= retryConfiguration.getAttempts()) {
          throw e;
        }
        long backoffPeriod = getBackoffPeriodMs(retries);
        try {
          Thread.sleep(backoffPeriod);
        } catch (InterruptedException ignored) {
        }
        log.warn(
            "Got error when querying for metrics. Retrying request (current attempt: {}, max "
                + "attempts: {}, last backoff period: {}ms)",
            retries,
            retryConfiguration.getAttempts(),
            backoffPeriod,
            e);
      }
    }
    String metricSetListId = UUID.randomUUID() + "";

    storageService.storeObject(
        storageAccount, ObjectType.METRIC_SET_LIST, metricSetListId, metricSetList);

    return metricSetListId;
  }

  private long getBackoffPeriodMs(int retryAttemptNumber) {
    // The retries range from 1..max, but we want the backoff periods to range from Math.pow(2,
    // 0)..Math.pow(2, max-1).
    return (long) Math.pow(2, (retryAttemptNumber - 1))
        * retryConfiguration.getBackoffPeriodMultiplierMs();
  }

  private boolean isRetryable(RetrofitError e) {
    if (isNetworkError(e)) {
      // retry in case of network errors
      return true;
    }
    if (e.getResponse() == null) {
      // We don't have a network error, but the response is null. It's better to not retry these.
      return false;
    }
    HttpStatus responseStatus = HttpStatus.resolve(e.getResponse().getStatus());
    if (responseStatus == null) {
      return false;
    }
    return retryConfiguration.getStatuses().contains(responseStatus)
        || retryConfiguration.getSeries().contains(responseStatus.series());
  }

  private boolean isNetworkError(RetrofitError e) {
    return e.getKind() == RetrofitError.Kind.NETWORK
        || (e.getResponse() == null && e.getCause() instanceof IOException);
  }

  public Map<String, ?> processQueryAndReturnMap(
      AccountCredentials metricsServiceCredentials,
      AccountCredentials storageServiceCredentials,
      CanaryConfig canaryConfig,
      CanaryMetricConfig canaryMetricConfig,
      int metricIndex,
      CanaryScope canaryScope,
      boolean dryRun)
      throws IOException {
    if (canaryConfig == null) {
      canaryConfig = CanaryConfig.builder().metric(canaryMetricConfig).build();
    }

    if (dryRun) {

      MetricsService metricsService = metricsRepo.getRequiredOne(metricsServiceCredentials);

      String query =
          metricsService.buildQuery(
              metricsServiceCredentials, canaryConfig, canaryMetricConfig, canaryScope);

      return Collections.singletonMap("query", query);
    } else {
      String metricSetListId =
          executeQuery(
              metricsServiceCredentials,
              storageServiceCredentials,
              canaryConfig,
              metricIndex,
              canaryScope);

      return Collections.singletonMap("metricSetListId", metricSetListId);
    }
  }

  public TaskResult executeQueryAndProduceTaskResult(
      AccountCredentials metricsAccount,
      AccountCredentials storageAccount,
      CanaryConfig canaryConfig,
      int metricIndex,
      CanaryScope canaryScope) {
    try {
      Map<String, ?> outputs =
          processQueryAndReturnMap(
              metricsAccount,
              storageAccount,
              canaryConfig,
              null /* canaryMetricConfig */,
              metricIndex,
              canaryScope,
              false /* dryRun */);

      return TaskResult.builder(ExecutionStatus.SUCCEEDED).outputs(outputs).build();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
