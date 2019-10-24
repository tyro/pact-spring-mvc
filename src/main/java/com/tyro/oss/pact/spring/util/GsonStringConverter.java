/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 - 2019 Tyro Payments Pty Ltd
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
package com.tyro.oss.pact.spring.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class GsonStringConverter implements ObjectStringConverter {

    private static final Logger LOG = LoggerFactory.getLogger(GsonStringConverter.class);

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    private Gson gson;

    public GsonStringConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T fromString(String message, Class<T> clazz) {
        try {
            return gson.fromJson(message, clazz);
        } catch (JsonSyntaxException jsonSyntaxException) {
            LOG.error("Error parsing JSON into a " + clazz.getCanonicalName());
            throw jsonSyntaxException;
        }
    }

    @Override
    public <T> T fromString(String message, Type type) {
        try {
            return gson.fromJson(message, type);
        } catch (JsonSyntaxException jsonSyntaxException) {
            LOG.error("Error parsing JSON into a" + type.toString());
            throw jsonSyntaxException;
        }
    }

    @Override
    public String fromObject(Object object) {
        return gson.toJson(object);
    }

    @Override
    public String fromObject(Object object, Type type) {
        return gson.toJson(object, type);
    }
}
