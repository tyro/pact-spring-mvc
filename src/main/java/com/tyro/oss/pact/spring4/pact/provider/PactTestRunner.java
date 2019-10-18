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

import com.google.gson.GsonBuilder;
import com.tyro.oss.pact.spring4.pact.model.ObjectStringConverterSource;
import com.tyro.oss.pact.spring4.pact.model.Pact;
import com.tyro.oss.pact.spring4.pact.model.Pact.Workflow;
import com.tyro.oss.pact.spring4.pact.provider.annotations.PactDefinition;
import com.tyro.oss.pact.spring4.pact.provider.annotations.ProviderState;
import com.tyro.oss.pact.spring4.pact.provider.annotations.WithPactFilter;
import com.tyro.oss.pact.spring4.pact.provider.annotations.WithPactResolver;
import com.tyro.oss.pact.spring4.util.GsonStringConverter;
import com.tyro.oss.pact.spring4.util.ObjectStringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;
import java.util.*;

import static java.util.function.Predicate.isEqual;

public class PactTestRunner extends SpringJUnit4ClassRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PactTestRunner.class);

    private List<FrameworkMethod> tests;

    private Method performInteractionMethod;

    private Map<String, Method> providerStateMethods;

    private ObjectStringConverter jsonConverter;

    private PactFilter pactFilter;

    private ConsumerFilter consumerFilter;

    public PactTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (tests == null) {
            try {
                List<Pact> pacts = initialiseAndDownloadPacts(); // computeTestMethods is called from the super constructor, hence initialisation here
                tests = getTests(pacts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return tests;
    }

    @Override
    protected String testName(FrameworkMethod method) {
        PactFrameworkMethod pactFrameworkMethod = (PactFrameworkMethod) method;
        return testNameForWorkflow(pactFrameworkMethod);
    }

    private String testNameForWorkflow(PactFrameworkMethod pactFrameworkMethod) {
        return pactFrameworkMethod.getWorkflow().getId()
                + " Pact("
                + pactFrameworkMethod.getPactVersion()
                + ")";
    }

    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                PactFrameworkMethod pactFrameworkMethod = (PactFrameworkMethod) method;
                if (pactFrameworkMethod.shouldExclude()) {
                    LOG.warn("Test has been excluded. Test will show as passed but was *NOT* run.");
                    return;
                }
                setUpProviderState(test, pactFrameworkMethod.getWorkflow());
                pactFrameworkMethod.invokeExplosively(test, pactFrameworkMethod.getWorkflow().getInteractions());
            }
        };
    }

    private List<Pact> initialiseAndDownloadPacts() throws Exception {
        Class<?> clazz = getTestClass().getJavaClass();
        assertTestClassIsSubclassOfPactTest(clazz);

        performInteractionMethod = getPerformInteractionMethod(clazz);

        providerStateMethods = getProviderStateMethods(clazz);

        jsonConverter = getJsonConverter(getPactDefinition(clazz));

        pactFilter = getPactFilter(clazz);

        consumerFilter = new ConsumerFilter(System.getProperty("pact.consumer.to.verify"));

        return getPactResolver(clazz).resolvePacts(getPactDefinition(clazz), jsonConverter);
    }

    private void assertTestClassIsSubclassOfPactTest(Class<?> clazz) {
        if (!PactTestBase.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("PactTestRunner must be used to run subclasses of PactTest");
        }
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

    private Method getPerformInteractionMethod(Class<?> clazz) throws Exception {
        return clazz.getMethod("performInteraction", List.class);
    }

    private List<FrameworkMethod> getTests(List<Pact> pacts) throws Exception {
        Class<?> clazz = getTestClass().getJavaClass();

        PactDefinition pactDef = getPactDefinition(clazz);

        Set<String> runOnly = new HashSet<>(Arrays.asList(pactDef.runOnly()));

        List<FrameworkMethod> testMethods = new ArrayList<>();

        for (Pact pact : pacts) {
            if (pactFilter.shouldExcludePact(clazz, pact)) {
                continue;
            }

            boolean shouldExclude = consumerFilter.shouldExcludePact(clazz);

            List<Workflow> uniqueWorkflows = getUniqueWorkflows(pact);

            for (Workflow workflow : uniqueWorkflows) {
                if (!pactFilter.shouldExcludeInteractionOrWorkflow(clazz, pact, workflow.getId())) {
                    if (runOnly.isEmpty() || runOnly.contains(workflow.getId())) {
                        testMethods.add(new PactFrameworkMethod(pact.getDisplayName(), workflow, shouldExclude));
                    }
                }
            }
        }
        return testMethods;
    }

    private List<Workflow> getUniqueWorkflows(Pact pact) {
        List<Workflow> uniqueWorkflows = new ArrayList<>();

        for (Workflow workflow : pact.getWorkFlows().values()) {
            Workflow existingWorkflow = uniqueWorkflows.stream().filter(isEqual(workflow)).findFirst().orElse(null);

            if (existingWorkflow != null) {
                LOG.info("Workflow " + workflow.getId() + " is a duplicate of " + existingWorkflow.getId() + " and will not be replayed");
            } else {
                uniqueWorkflows.add(workflow);
            }
        }

        return uniqueWorkflows;
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

    private void setUpProviderState(Object testInstance, Workflow workflow) throws Exception {
        for (Pact.ProviderState providerState : workflow.getProviderStates()) {
            if (testInstance instanceof ObjectStringConverterSource) {
                providerState.setJsonConverter(((ObjectStringConverterSource) testInstance).getConverter());
            } else {
                providerState.setJsonConverter(jsonConverter);
            }
            Method providerStateSetupMethod = providerStateMethods.get(providerState.getDescription());
            if (providerStateSetupMethod == null) {
                throw new IllegalStateException("Cannot find a setup method for provider state " + providerState.getDescription());
            }
            providerStateSetupMethod.invoke(testInstance, providerState.getStates(providerStateSetupMethod.getGenericParameterTypes()));
        }
    }

    private ObjectStringConverter getJsonConverter(PactDefinition pactDefinition) {
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

    private class PactFrameworkMethod extends FrameworkMethod {

        private final String pactVersion;
        private final Workflow workflow;
        private final boolean exclude;

        PactFrameworkMethod(String pactVersion, Workflow workflow, boolean exclude) {
            super(performInteractionMethod);
            this.pactVersion = pactVersion;
            this.workflow = workflow;
            this.exclude = exclude;
        }

        public String getPactVersion() {
            return pactVersion;
        }

        public Workflow getWorkflow() {
            return workflow;
        }

        public boolean shouldExclude() {
            return this.exclude;
        }

        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
