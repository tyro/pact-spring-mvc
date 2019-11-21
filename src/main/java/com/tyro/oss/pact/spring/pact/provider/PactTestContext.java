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
package com.tyro.oss.pact.spring.pact.provider;

import com.tyro.oss.pact.spring.pact.model.Pact;
import com.tyro.oss.pact.spring.util.ObjectStringConverter;

import java.lang.reflect.Method;
import java.util.Map;

public class PactTestContext {

    private final String pactVersion;
    private final Pact.Workflow workflow;
    private final Map<String, Method> providerStateMethods;
    private final ObjectStringConverter objectStringConverter;
    private final boolean exclude;

    public PactTestContext(String pactVersion, Pact.Workflow workflow, ObjectStringConverter objectStringConverter, Map<String, Method> providerStateMethods, boolean exclude) {
        this.pactVersion = pactVersion;
        this.workflow = workflow;
        this.providerStateMethods = providerStateMethods;
        this.exclude = exclude;
        this.objectStringConverter = objectStringConverter;
    }

    public String getPactVersion() {
        return pactVersion;
    }

    public Pact.Workflow getWorkflow() {
        return workflow;
    }

    public Method getProviderState(String description) {
        return providerStateMethods.get(description);
    }

    public ObjectStringConverter getObjectStringConverter() {
        return objectStringConverter;
    }

    public boolean shouldExclude() {
        return this.exclude;
    }
}
