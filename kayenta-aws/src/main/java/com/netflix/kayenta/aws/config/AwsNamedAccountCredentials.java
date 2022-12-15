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

package com.netflix.kayenta.aws.config;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.kayenta.security.AccountCredentials;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor // required for Spring binding
public class AwsNamedAccountCredentials extends AccountCredentials {

  @NotNull private String name;

  @NotNull @Singular private List<Type> supportedTypes;

  private String bucket;
  private String region;
  private String rootFolder;
  private String endpoint;
  private String proxyHost;
  private String proxyPort;
  private String proxyProtocol;

  private String profileName;
  private ExplicitAwsCredentials explicitCredentials;

  @Data
  public static class ExplicitAwsCredentials {

    String accessKey;
    String secretKey;
    String sessionToken;
  }

  @JsonIgnore private transient AmazonS3 credentials;

  @Override
  public String getType() {
    return "aws";
  }
}
