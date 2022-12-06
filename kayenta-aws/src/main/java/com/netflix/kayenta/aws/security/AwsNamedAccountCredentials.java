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

package com.netflix.kayenta.aws.security;

import com.amazonaws.services.s3.AmazonS3;
import com.netflix.kayenta.aws.storage.S3StorageService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.storage.StorageService;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class AwsNamedAccountCredentials extends AccountCredentials {

    @NotNull
    private String name;

    @NotNull
    @Singular
    private List<Type> supportedTypes;


    private String bucket;
    private String region;
    private String rootFolder;
    private String endpoint;
    private String proxyHost;
    private String proxyPort;
    private String proxyProtocol;

    private String profileName;
    private ExplicitAwsCredentials explicitCredentials;
    private S3StorageService supportingService;

    @Data
    public static class ExplicitAwsCredentials {

        String accessKey;
        String secretKey;
        String sessionToken;
    }

    private AmazonS3 credentials;

    @Override
    public String getType() {
        return "aws";
    }

    @Override
    public S3StorageService getServiceForType(Type type) {
        return supportingService;
    }
}
