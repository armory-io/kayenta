/*
 * Copyright 2023 Netflix, Inc.
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

package com.netflix.kayenta.sql.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Level;
import javax.persistence.AttributeConverter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Log
@Component
public class JsonConverter implements AttributeConverter<Object, String> {
  private static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    // optional: customisations to the object mapper
    //        MAPPER.registerModule(new JodaModule());
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static ObjectMapper getMapper() {
    return MAPPER;
  }

  @Override
  public String convertToDatabaseColumn(Object attribute) {
    final ObjectMapper mapper = getMapper();
    if (attribute == null) {
      return "";
    }
    try {
      return attribute.getClass().getName() + "|" + mapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.log(Level.SEVERE, "Exceptione converting to database column", e);
      return null;
    }
  }

  @Override
  public Object convertToEntityAttribute(String dbData) {
    final ObjectMapper mapper = getMapper();
    try {
      if (StringUtils.isBlank(dbData)) {
        return null;
      }
      final String[] parts = dbData.split("\\|", 2);
      return mapper.readValue(parts[1], Class.forName(parts[0]));
    } catch (Exception e) {
      log.log(Level.SEVERE, "Exceptione converting to object", e);
      return null;
    }
  }
}
