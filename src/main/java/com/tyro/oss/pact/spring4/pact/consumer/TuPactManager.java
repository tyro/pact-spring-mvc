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
package com.tyro.oss.pact.spring4.pact.consumer;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;


import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.unitils.util.AnnotationUtils;


import com.google.gson.GsonBuilder;

import com.tyro.oss.pact.spring4.pact.consumer.annotations.PactServer;
import com.tyro.oss.pact.spring4.util.GsonStringConverter;
import com.tyro.oss.pact.spring4.util.ObjectStringConverter;

public class TuPactManager implements MethodRule {

    protected String outputPath = "./target/pact";

    protected ObjectStringConverter converter = new GsonStringConverter(new GsonBuilder().create());
    private MediaType contentType = MediaType.APPLICATION_JSON;

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, Object testInstance) {
        final Set<Field> serverFields = AnnotationUtils.getFieldsAnnotatedWith(testInstance.getClass(), PactServer.class);
        final Set<TuPactRecordingServer> servers = new HashSet<>();
        for (Field serverField : serverFields) {
            serverField.setAccessible(true);
            try {
                final PactServer annotation = serverField.getAnnotation(PactServer.class);
                final File fileName = new File(outputPath, annotation.provider().replace("-", "_") + "_pacts.json");

                final String expression = annotation.bean();
                final RestTemplate restTemplate = evaluateRestTemplate(testInstance, expression);

                final TuPactRecordingServer server = createServer(fileName, restTemplate);
                servers.add(server);
                serverField.set(testInstance, server);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not access field " + serverField.getName() + " annotated with @" + PactServer.class.getSimpleName(), e);
            }
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (TuPactRecordingServer server : servers) {
                    server.startWorkflow(method.getName());
                }

                Throwable throwable = null;
                try {
                    base.evaluate();
                } catch (Throwable t) {
                    throwable = t;
                } finally {
                    for (TuPactRecordingServer server : servers) {
                        try {
                            server.close();
                        } catch (Throwable t) {
                            if (throwable == null) {
                                throwable = t;
                            }
                        }
                    }
                    if (throwable != null) {
                        throw throwable;
                    }
                }
            }
        };
    }

    public TuPactManager useOutputPath(String path) {
        this.outputPath = path;
        return this;
    }

    public TuPactManager useConverter(ObjectStringConverter converter) {
        this.converter = converter;
        return this;
    }

    public TuPactManager useContentType(MediaType contentType) {
        this.contentType = contentType;
        return this;
    }

    private RestTemplate evaluateRestTemplate(Object testInstance, String expression) {
        final ApplicationContext applicationContext = getApplicationContext(testInstance);

        if (expression.startsWith("#")) {
            try {
                final BeanExpressionContext beanContext = new BeanExpressionContext((ConfigurableBeanFactory) applicationContext.getAutowireCapableBeanFactory(), null);
                return (RestTemplate) new StandardBeanExpressionResolver().evaluate(expression, beanContext);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not resolve expression '" + expression + "' into an org.springframework.web.client.RestTemplate.", e);
            }
        } else {
            try {
                return (RestTemplate) applicationContext.getBean(expression);
            } catch (BeansException e) {
                throw new IllegalArgumentException("Could not resolve bean name '" + expression + "' into an org.springframework.web.client.RestTemplate.", e);
            }
        }
    }

    private ApplicationContext getApplicationContext(Object testInstance) {
        try {
            return (ApplicationContext) ReflectionTestUtils.getField(testInstance, "applicationContext");
        } catch (Exception e) {
            throw new IllegalStateException("@TuPactManager requires application context to be present, are you extending AbstractJUnit4SpringContextTests?");
        }
    }

    protected TuPactRecordingServer createServer(File file, RestTemplate restTemplate) throws IllegalAccessException {
        return TuPactRecordingServer.createServer(restTemplate, file, this.converter, this.contentType);
    }
}
