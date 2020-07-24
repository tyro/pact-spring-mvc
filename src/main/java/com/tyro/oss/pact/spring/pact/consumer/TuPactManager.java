/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 - 2020 Tyro Payments Pty Ltd
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
package com.tyro.oss.pact.spring.pact.consumer;

import com.google.gson.GsonBuilder;
import com.tyro.oss.pact.spring.pact.consumer.annotations.PactServer;
import com.tyro.oss.pact.spring.util.GsonStringConverter;
import com.tyro.oss.pact.spring.util.ObjectStringConverter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.unitils.util.AnnotationUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class TuPactManager implements BeforeEachCallback, AfterEachCallback {

    protected String outputPath = "./target/pact";

    protected ObjectStringConverter converter = new GsonStringConverter(new GsonBuilder().create());
    private MediaType contentType = MediaType.APPLICATION_JSON;

    private Set<TuPactRecordingServer> servers;

    @Override
    public void beforeEach(ExtensionContext context) {
        servers = new HashSet<>();

        context.getTestClass().ifPresent(testClass -> {
            Set<Field> serverFields = AnnotationUtils.getFieldsAnnotatedWith(testClass, PactServer.class);
            for (Field serverField : serverFields) {
                serverField.setAccessible(true);
                try {
                    PactServer annotation = serverField.getAnnotation(PactServer.class);
                    File fileName = new File(outputPath, annotation.provider().replace("-", "_") + "_pacts.json");

                    String expression = annotation.bean();
                    RestTemplate restTemplate = evaluateRestTemplate(context.getTestInstance(), expression);

                    TuPactRecordingServer server = createServer(fileName, restTemplate);
                    servers.add(server);
                    serverField.set(context.getTestInstance(), server);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not access field " + serverField.getName() + " annotated with @" + PactServer.class.getSimpleName(), e);
                }
            }
        });

        context.getTestMethod().ifPresent(testMethod -> {
            servers.forEach(server -> server.startWorkflow(testMethod.getName()));
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        servers.forEach(TuPactRecordingServer::close);
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
