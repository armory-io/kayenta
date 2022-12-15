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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MapBackedAccountCredentialsRepository implements AccountCredentialsRepository {

  private final Map<String, AccountCredentials> accountNameToCredentialsMap =
      new ConcurrentHashMap<>();

  @Override
  public AccountCredentials save(AccountCredentials entity) {
    return accountNameToCredentialsMap.put(entity.getName(), entity);
  }

  @Override
  public <S extends AccountCredentials> Iterable<S> saveAll(Iterable<S> entities) {
    entities.forEach(s -> accountNameToCredentialsMap.put(s.getName(), s));
    return entities;
  }

  @Override
  public Optional<AccountCredentials> findById(String s) {
    return Optional.ofNullable(accountNameToCredentialsMap.get(s));
  }

  @Override
  public boolean existsById(String s) {
    return accountNameToCredentialsMap.containsKey(s);
  }

  @Override
  public Iterable<AccountCredentials> findAll() {
    return accountNameToCredentialsMap.values();
  }

  @Override
  public Iterable<AccountCredentials> findAllById(Iterable<String> strings) {
    Collection<String> accounts =
        StreamSupport.stream(strings.spliterator(), false).collect(Collectors.toList());
    return accountNameToCredentialsMap.values().stream()
        .filter(it -> accounts.contains(it.getName()))
        .collect(Collectors.toSet());
  }

  @Override
  public long count() {
    return accountNameToCredentialsMap.size();
  }

  @Override
  public void deleteById(String s) {
    accountNameToCredentialsMap.remove(s);
  }

  @Override
  public void delete(AccountCredentials entity) {

    accountNameToCredentialsMap.remove(entity.getName());
  }

  @Override
  public void deleteAll(Iterable<? extends AccountCredentials> entities) {
    entities.forEach(accountNameToCredentialsMap::remove);
  }

  @Override
  public void deleteAll() {
    accountNameToCredentialsMap.clear();
  }
}
