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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.List;


import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriUtils;


import com.tyro.oss.pact.spring4.pact.model.Pact;
import com.tyro.oss.pact.spring4.pact.provider.annotations.WithSpringSecurity;

@RunWith(PactTestRunner.class)
@WebAppConfiguration
@Transactional(propagation = Propagation.SUPPORTS)
public abstract class PactTestBase extends AbstractJUnit4SpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(PactTestBase.class);

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    private static List<ResponseBodyMatcher> responseBodyMatchers = new ArrayList<>(asList(
            new JsonResponseBodyMatcher(),
            new StatusCodeNoContentResponseBodyMatcher(),
            new DefaultResponseBodyMatcher()
    ));

    @Before
    public void setup() {
        DefaultMockMvcBuilder mockMvcBuilder = MockMvcBuilders.webAppContextSetup(wac);

        WithSpringSecurity withSpringSecurityAnnotation = this.getClass().getAnnotation(WithSpringSecurity.class);

        if (withSpringSecurityAnnotation != null) {
            mockMvcBuilder.apply(springSecurity());
        }

        initialiseMockMvcBuilder(mockMvcBuilder);
        mockMvc = mockMvcBuilder.build();
    }

    public void performInteraction(List<Pact.Interaction> interactions) throws Exception {
        for (Pact.Interaction interaction : interactions) {
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
        uri = UriUtils.decode(uri, "UTF-8");

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
            assertThat("Pact test only supports single-valued headers", pactResponse.getHeaders().get(header).size(), is(1));
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
