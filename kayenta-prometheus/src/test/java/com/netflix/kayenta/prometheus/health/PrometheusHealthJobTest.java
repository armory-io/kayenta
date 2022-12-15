/*
 * Copyright 2020 Playtika.
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

package com.netflix.kayenta.prometheus.health;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.kayenta.prometheus.config.PrometheusManagedAccount;
import com.netflix.kayenta.prometheus.service.PrometheusRemoteService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.MapBackedAccountCredentialsRepository;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Status;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PrometheusHealthJobTest {

  PrometheusManagedAccount ACCOUNT_1 =
      PrometheusManagedAccount.builder().name(PROM_ACCOUNT_1).baseUrl("any").build();
  PrometheusManagedAccount ACCOUNT_2 =
      PrometheusManagedAccount.builder().name(PROM_ACCOUNT_2).baseUrl("any").build();
  private static final String PROM_ACCOUNT_1 = "a1";
  private static final String PROM_ACCOUNT_2 = "a2";
  @Mock PrometheusRemoteService PROM_REMOTE_1;
  @Mock PrometheusRemoteService PROM_REMOTE_2;

  AccountCredentialsRepository accountCredentialsRepository =
      new MapBackedAccountCredentialsRepository();
  @Mock PrometheusHealthCache healthCache;

  PrometheusHealthJob healthJob;

  @Before
  public void setUp() {
    ACCOUNT_1.setPrometheusRemoteService(PROM_REMOTE_1);
    ACCOUNT_2.setPrometheusRemoteService(PROM_REMOTE_2);
    accountCredentialsRepository.save(ACCOUNT_2);
    accountCredentialsRepository.save(ACCOUNT_1);
    healthJob = new PrometheusHealthJob(accountCredentialsRepository, healthCache);
  }

  @Test
  public void allRemotesAreUp() {
    when(PROM_REMOTE_1.isHealthy()).thenReturn("OK");
    when(PROM_REMOTE_2.isHealthy()).thenReturn("OK");

    healthJob.run();

    verify(healthCache)
        .setHealthStatuses(
            Arrays.asList(
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_1)
                    .status(Status.UP)
                    .build(),
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_2)
                    .status(Status.UP)
                    .build()));
  }

  @Test
  public void allRemotesAreDown() {
    when(PROM_REMOTE_1.isHealthy()).thenThrow(new RuntimeException("test 1"));
    when(PROM_REMOTE_2.isHealthy()).thenThrow(new RuntimeException("test 2"));
    healthJob.run();

    verify(healthCache)
        .setHealthStatuses(
            Arrays.asList(
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_1)
                    .status(Status.DOWN)
                    .errorDetails("java.lang.RuntimeException: test 1")
                    .build(),
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_2)
                    .status(Status.DOWN)
                    .errorDetails("java.lang.RuntimeException: test 2")
                    .build()));
  }

  @Test
  public void oneRemoteIsDown() {
    when(PROM_REMOTE_1.isHealthy()).thenReturn("OK");
    when(PROM_REMOTE_2.isHealthy()).thenThrow(new RuntimeException("test 2"));

    healthJob.run();

    verify(healthCache)
        .setHealthStatuses(
            Arrays.asList(
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_1)
                    .status(Status.UP)
                    .build(),
                PrometheusHealthJob.PrometheusHealthStatus.builder()
                    .accountName(PROM_ACCOUNT_2)
                    .status(Status.DOWN)
                    .errorDetails("java.lang.RuntimeException: test 2")
                    .build()));
  }
}
