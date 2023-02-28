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

package com.netflix.kayenta.storage;

import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.util.List;
import java.util.Map;

public interface StorageService<Y> {
  boolean servicesAccount(String accountName);

  <T> T loadObject(
      AccountCredentials<Y> accountCredentials, ObjectType objectType, String objectKey)
      throws IllegalArgumentException, NotFoundException;

  <T> void storeObject(
      AccountCredentials<Y> accountCredentials,
      ObjectType objectType,
      String objectKey,
      T obj,
      String filename,
      boolean isAnUpdate);

  void deleteObject(AccountCredentials<Y> credentials, ObjectType objectType, String objectKey);

  List<Map<String, Object>> listObjectKeys(
      AccountCredentials<Y> credentials,
      ObjectType objectType,
      List<String> applications,
      boolean skipIndex);

  default <T> void storeObject(
      AccountCredentials<Y> credentials, ObjectType objectType, String objectKey, T obj) {
    storeObject(credentials, objectType, objectKey, obj, null, true);
  }

  default List<Map<String, Object>> listObjectKeys(
      AccountCredentials<Y> credentials, ObjectType objectType) {
    return listObjectKeys(credentials, objectType, null, false);
  }
}
