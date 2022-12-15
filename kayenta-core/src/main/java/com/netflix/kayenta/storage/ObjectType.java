package com.netflix.kayenta.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryExecutionStatusResponse;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricSetPair;
import java.util.List;
import lombok.Data;

@Data
public class ObjectType {

  public static ObjectType CANARY_CONFIG =
      new ObjectType(new TypeReference<CanaryConfig>() {}, "canary_config", "canary_config.json");
  public static ObjectType CANARY_RESULT_ARCHIVE =
      new ObjectType(
          new TypeReference<CanaryExecutionStatusResponse>() {},
          "canary_archive",
          "canary_archive.json");
  public static ObjectType METRIC_SET_LIST =
      new ObjectType(new TypeReference<List<MetricSet>>() {}, "metrics", "metric_sets.json");
  public static ObjectType METRIC_SET_PAIR_LIST =
      new ObjectType(
          new TypeReference<List<MetricSetPair>>() {}, "metric_pairs", "metric_set_pairs.json");
  private final TypeReference<?> typeReference;
  private final String group;
  private final String defaultFilename;

  public ObjectType(TypeReference<?> typeReference, String group, String defaultFilename) {
    this.typeReference = typeReference;
    this.group = group;
    this.defaultFilename = defaultFilename;
  }
}
