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
package com.tyro.oss.pact.spring4.pact.provider;

import com.tyro.oss.pact.spring4.pact.provider.annotations.PactDefinition;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ConsumerFilter {

    private String consumerToInclude;

    public ConsumerFilter(String consumerToInclude) {
        this.consumerToInclude = consumerToInclude;
    }

    public boolean shouldExcludePact(Class<?> clazz) {
        PactDefinition pactDefinition = AnnotationUtils.findAnnotation(clazz, PactDefinition.class);
        boolean pactShouldBeIncluded = isBlank(consumerToInclude) || Objects.equals(consumerFrom(pactDefinition), consumerToInclude);
        return !pactShouldBeIncluded;
    }

    private String consumerFrom(PactDefinition pactDefinition) {
        return pactDefinition != null ? pactDefinition.consumer() : "";
    }
}
