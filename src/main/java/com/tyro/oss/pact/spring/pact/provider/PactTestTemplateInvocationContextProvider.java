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

import com.google.gson.GsonBuilder;
import com.tyro.oss.pact.spring.pact.model.ObjectStringConverterSource;
import com.tyro.oss.pact.spring.pact.model.Pact;
import com.tyro.oss.pact.spring.pact.provider.annotations.PactDefinition;
import com.tyro.oss.pact.spring.pact.provider.annotations.ProviderState;
import com.tyro.oss.pact.spring.pact.provider.annotations.WithPactFilter;
import com.tyro.oss.pact.spring.pact.provider.annotations.WithPactResolver;
import com.tyro.oss.pact.spring.util.GsonStringConverter;
import com.tyro.oss.pact.spring.util.ObjectStringConverter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.function.Predicate.isEqual;

public class PactTestTemplateInvocationContextProvider implements TestTemplateInvocationContextProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PactTestTemplateInvocationContextProvider.class);

    private PactFilter pactFilter;
    private ConsumerFilter consumerFilter;
    private ObjectStringConverter objectStringConverter;

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        try {
            List<Pact> pacts = initialiseAndDownloadPacts(context);
            return createTests(context, pacts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Pact> initialiseAndDownloadPacts(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        PactDefinition pactDefinition = getPactDefinition(testClass);

        objectStringConverter = getConverterSource(pactDefinition);

        pactFilter = getPactFilter(testClass);

        consumerFilter = new ConsumerFilter(System.getProperty("pact.consumer.to.verify"));

        return getPactResolver(testClass).resolvePacts(pactDefinition, objectStringConverter);
    }

    private PactDefinition getPactDefinition(Class<?> clazz) {
        PactDefinition pactDefinition = AnnotationUtils.findAnnotation(clazz, PactDefinition.class);
        if (pactDefinition == null) {
            throw new IllegalStateException("PactTest subclasses must be annotated with @PactDefinition");
        }
        return pactDefinition;
    }

    private PactFilter getPactFilter(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        WithPactFilter withPactFilter = AnnotationUtils.findAnnotation(clazz, WithPactFilter.class);

        if (withPactFilter == null) {
            return new NoOpPactFilter();
        } else {
            return withPactFilter.value().newInstance();
        }
    }

    private PactResolver getPactResolver(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        WithPactResolver withPactResolver = AnnotationUtils.findAnnotation(clazz, WithPactResolver.class);

        if (withPactResolver == null) {
            return new DefaultPactResolver();
        } else {
            return withPactResolver.value().newInstance();
        }
    }

    private Map<String, Method> getProviderStateMethods(Class<?> clazz) {
        Map<String, Method> providerStateMethods = new HashMap<>();

        for (Method method : clazz.getMethods()) {
            ProviderState annotation = AnnotationUtils.findAnnotation(method, ProviderState.class);
            if (annotation != null) {
                String state = annotation.value();
                if (StringUtils.isEmpty(state)) {
                    state = method.getName();
                }
                if (providerStateMethods.containsKey(state)) {
                    throw new IllegalStateException("There must be only one setup method per provider state");
                }
                providerStateMethods.put(state, method);
            }
        }

        return providerStateMethods;
    }

    private Stream<TestTemplateInvocationContext> createTests(ExtensionContext context, List<Pact> pacts) {
        Class<?> clazz = context.getRequiredTestClass();

        PactDefinition pactDef = getPactDefinition(clazz);

        Map<String, Method> providerStateMethods = getProviderStateMethods(clazz);

        Set<String> runOnly = new HashSet<>(Arrays.asList(pactDef.runOnly()));

        List<TestTemplateInvocationContext> testMethods = new ArrayList<>();

        for (Pact pact : pacts) {
            if (pactFilter.shouldExcludePact(clazz, pact)) {
                continue;
            }

            boolean shouldExclude = consumerFilter.shouldExcludePact(clazz);

            List<Pact.Workflow> uniqueWorkflows = getUniqueWorkflows(pact);

            for (Pact.Workflow workflow : uniqueWorkflows) {
                if (!pactFilter.shouldExcludeInteractionOrWorkflow(clazz, pact, workflow.getId())) {
                    if (runOnly.isEmpty() || runOnly.contains(workflow.getId())) {
                        testMethods.add(new PactTestTemplateInvocationContext(new PactTestContext(
                                        pact.getDisplayName(),
                                        workflow,
                                        objectStringConverter,
                                        providerStateMethods,
                                        shouldExclude)));
                    }
                }
            }
        }
        return testMethods.stream();
    }

    private List<Pact.Workflow> getUniqueWorkflows(Pact pact) {
        List<Pact.Workflow> uniqueWorkflows = new ArrayList<>();

        for (Pact.Workflow workflow : pact.getWorkFlows().values()) {
            Pact.Workflow existingWorkflow = uniqueWorkflows.stream().filter(isEqual(workflow)).findFirst().orElse(null);

            if (existingWorkflow != null) {
                LOG.info("Workflow " + workflow.getId() + " is a duplicate of " + existingWorkflow.getId() + " and will not be replayed");
            } else {
                uniqueWorkflows.add(workflow);
            }
        }

        return uniqueWorkflows;
    }

    private ObjectStringConverter getConverterSource(PactDefinition pactDefinition) {
        Class<? extends ObjectStringConverterSource> converterClass = pactDefinition.converterSource();

        if (converterClass == ObjectStringConverterSource.class) {
            return new GsonStringConverter(new GsonBuilder().create());
        } else {
            try {
                return converterClass.newInstance().getConverter();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Invalid ObjectStringConverterSource - Do you have a public no-arg constructor?", e);
            }
        }
    }

    public static class PactTestTemplateInvocationContext implements TestTemplateInvocationContext {

        private final PactTestContext pactTestContext;

        PactTestTemplateInvocationContext(PactTestContext pactTestContext) {
            this.pactTestContext = pactTestContext;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return format("%s Pact(%s)", pactTestContext.getWorkflow().getId(), pactTestContext.getPactVersion());
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return Collections.singletonList(new ParameterResolver() {
                @Override
                public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                    return parameterContext.getParameter().getType().equals(PactTestContext.class);
                }

                @Override
                public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                    return pactTestContext;
                }
            });
        }
    }
}
