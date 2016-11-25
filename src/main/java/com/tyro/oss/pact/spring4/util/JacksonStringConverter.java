/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 Tyro Payments Pty Ltd
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.tyro.oss.pact.spring4.util;


import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonStringConverter implements ObjectStringConverter {


    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private ObjectMapper objectMapper;

    public JacksonStringConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromString(String message, Class<T> clazz) {
        try {
            return objectMapper.readValue(message.getBytes(), clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public <T> T fromString(String message, Type type) {
        try {
            return objectMapper.readValue(message.getBytes(), objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String fromObject(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String fromObject(Object object, Type type) {
        try {
            return objectMapper.writerWithType(objectMapper.getTypeFactory().constructType(type)).writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
