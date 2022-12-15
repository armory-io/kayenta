package com.netflix.kayenta.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.canary.CanaryJudge;
import com.netflix.kayenta.canary.ExecutionMapper;
import com.netflix.kayenta.config.WebConfiguration;
import com.netflix.kayenta.metrics.MetricsServiceRepository;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.MapBackedAccountCredentialsRepository;
import com.netflix.kayenta.service.MetricSetPairListService;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.orca.pipeline.ExecutionLauncher;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootTest(
    classes = BaseControllerTest.TestControllersConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@RunWith(SpringRunner.class)
public abstract class BaseControllerTest {

  protected static final AccountCredentials CONFIGS_ACCOUNT =
      new MockAccountCredentials("configs-account", AccountCredentials.Type.CONFIGURATION_STORE);
  protected static final AccountCredentials METRICS_STORE =
      new MockAccountCredentials("metrics-store", AccountCredentials.Type.METRICS_STORE);
  protected static final AccountCredentials OBJECT_STORE =
      new MockAccountCredentials("object-store", AccountCredentials.Type.OBJECT_STORE);

  @MockBean StorageService storageService;
  @MockBean MetricSetPairListService metricSetPairListService;
  @MockBean ExecutionRepository executionRepository;
  @MockBean ExecutionLauncher executionLauncher;
  @Autowired ExecutionMapper executionMapper;

  @MockBean MetricsServiceRepository metricsServiceRepository;

  @MockBean(answer = Answers.RETURNS_MOCKS)
  Registry registry;

  @MockBean CanaryJudge canaryJudge;

  @Autowired private WebApplicationContext webApplicationContext;

  protected MockMvc mockMvc;

  @Before
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(this.webApplicationContext).alwaysDo(print()).build();
    when(storageService.appliesTo(CONFIGS_ACCOUNT)).thenReturn(true);
  }

  @EnableWebMvc
  @Import(WebConfiguration.class)
  @Configuration
  public static class TestControllersConfiguration {

    @Bean
    StorageServiceRepository storageServiceRepository(List<StorageService> storageServices) {
      return new StorageServiceRepository(accountCredentialsRepository(), storageServices);
    }

    @Bean
    @Scope("prototype")
    ExecutionMapper executionMapper(
        ExecutionRepository executionRepository,
        ExecutionLauncher executionLauncher,
        Registry registry) {
      return new ExecutionMapper(
          new ObjectMapper(),
          registry,
          "",
          Optional.empty(),
          executionLauncher,
          executionRepository,
          false);
    }

    @Bean
    AccountCredentialsRepository accountCredentialsRepository() {
      MapBackedAccountCredentialsRepository repo = new MapBackedAccountCredentialsRepository();
      repo.save(CONFIGS_ACCOUNT);
      repo.save(METRICS_STORE);
      repo.save(OBJECT_STORE);
      return repo;
    }
  }
}
