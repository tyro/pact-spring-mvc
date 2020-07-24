/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 - 2020 Tyro Payments Limited
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

import com.tyro.oss.pact.spring.pact.model.ObjectStringConverterSource;
import com.tyro.oss.pact.spring.pact.model.Pact;
import com.tyro.oss.pact.spring.pact.provider.annotations.WithSpringSecurity;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class PactTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(PactTestBase.class);
    private static List<ResponseBodyMatcher> responseBodyMatchers = new ArrayList<>(asList(
            new JsonResponseBodyMatcher(),
            new StatusCodeNoContentResponseBodyMatcher(),
            new DefaultResponseBodyMatcher()
    ));

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @BeforeEach
    protected void setup() {
        DefaultMockMvcBuilder mockMvcBuilder = MockMvcBuilders.webAppContextSetup(wac);
        WithSpringSecurity withSpringSecurityAnnotation = this.getClass().getAnnotation(WithSpringSecurity.class);

        if (withSpringSecurityAnnotation != null) {
            mockMvcBuilder.apply(springSecurity());
        }

        initialiseMockMvcBuilder(mockMvcBuilder);
        mockMvc = mockMvcBuilder.build();
    }

    @TestTemplate
    @ExtendWith(PactTestTemplateInvocationContextProvider.class)
    protected void pactTests(PactTestContext context) throws Exception {
        if (context.shouldExclude()) {
            LOG.warn("Test has been excluded. Test will show as passed but was *NOT* run.");
            return;
        }
        setUpProviderState(context);
        performInteraction(context);
    }

    protected void setUpProviderState(PactTestContext context) throws Exception {
        for (Pact.ProviderState providerState : context.getWorkflow().getProviderStates()) {
            if (this instanceof ObjectStringConverterSource) {
                providerState.setJsonConverter(((ObjectStringConverterSource) this).getConverter());
            } else {
                providerState.setJsonConverter(context.getObjectStringConverter());
            }
            Method providerStateSetupMethod = context.getProviderState(providerState.getDescription());
            if (providerStateSetupMethod == null) {
                throw new IllegalStateException("Cannot find a setup method for provider state " + providerState.getDescription());
            }
            providerStateSetupMethod.setAccessible(true);
            providerStateSetupMethod.invoke(this, providerState.getStates(providerStateSetupMethod.getGenericParameterTypes()));
        }
    }

    protected void performInteraction(PactTestContext context) throws Exception {
        for (Pact.Interaction interaction : context.getWorkflow().getInteractions()) {
            LOG.info("Testing interaction [" + interaction + "]");
            Pact.InteractionRequest pactRequest = interaction.getRequest();
            Pact.InteractionResponse pactExpectation = interaction.getResponse();

            RequestBuilder requestBuilder = toMockMvcRequest(pactRequest);

            ResultActions actualResponse = mockMvc.perform(requestBuilder);
            assertResponse(actualResponse, pactExpectation);
        }
    }

    public void addResponseBodyMatcher(ResponseBodyMatcher matcher) {
        responseBodyMatchers.add(0, matcher);
    }

    protected void initialiseMockMvcBuilder(DefaultMockMvcBuilder mockMvcBuilder) {
        // hook to allow clients to perform any custom setup on the mockMvcBuilder
    }

    protected abstract String getServletContextPathWithoutTrailingSlash();

    protected RequestBuilder toMockMvcRequest(Pact.InteractionRequest request) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = createRequestBuilderWithMethodAndUri(request)
                .headers(customiseHeaders(request.getHeaders()))
                .contentType(MediaType.APPLICATION_JSON);
        Object requestBody = request.getBody();
        return requestBody == null ? requestBuilder : requestBuilder.content(requestBody.toString());
    }

    protected HttpHeaders customiseHeaders(HttpHeaders headers) {
        return headers;
    }

    private void assertResponse(ResultActions actualResponse, Pact.InteractionResponse pactExpectation) throws Exception {
        assertStatus(actualResponse, pactExpectation);
        assertHeaders(actualResponse, pactExpectation);
        assertContent(actualResponse, pactExpectation);
    }

    protected MockHttpServletRequestBuilder createRequestBuilderWithMethodAndUri(Pact.InteractionRequest request) throws Exception {
        String uri = request.getUri().contains(getServletContextPathWithoutTrailingSlash())
                ? StringUtils.substringAfter(request.getUri(), getServletContextPathWithoutTrailingSlash())
                : request.getUri();
        uri = UriUtils.decode(uri, UTF_8);

        switch (request.getMethod()) {
            case GET:
                return get(uri);
            case POST:
                return post(uri);
            case PUT:
                return put(uri);
            case DELETE:
                return delete(uri);
            default:
                throw new RuntimeException("Unsupported method " + request.getMethod());
        }
    }

    private void assertStatus(ResultActions actualResponse, Pact.InteractionResponse pactExpectation) throws Exception {
        actualResponse.andExpect(status().is(pactExpectation.getStatus()));
    }

    private void assertHeaders(ResultActions actualResponse, Pact.InteractionResponse pactResponse) throws Exception {
        for (String header : pactResponse.getHeaders().keySet()) {
            assertEquals(1, pactResponse.getHeaders().get(header).size(), "Pact test only supports single-valued headers");
            String value = pactResponse.getHeaders().get(header).get(0);
            if (header.equalsIgnoreCase("Content-Type")) {
                actualResponse.andExpect(header().string(header, startsWith(value)));
            } else {
                actualResponse.andExpect(header().string(header, value));
            }
        }
    }

    private void assertContent(ResultActions actualResponse, Pact.InteractionResponse pactExpectation) throws Exception {
        ResponseBodyMatcher responseBodyMatcher = findResponseBodyMatcher(actualResponse, pactExpectation);
        if (responseBodyMatcher != null) {
            responseBodyMatcher.assertContent(actualResponse, pactExpectation);
        } else {
            throw new AssertionError("No ResponseBodyMatchers found to assertContent!");
        }
    }

    protected ResponseBodyMatcher findResponseBodyMatcher(ResultActions actualResponse, Pact.InteractionResponse pactExpectation) {
        for (ResponseBodyMatcher responseBodyMatcher : responseBodyMatchers) {
            if (responseBodyMatcher.canHandle(actualResponse, pactExpectation)) {
                return responseBodyMatcher;
            }
        }
        return null;
    }
}
