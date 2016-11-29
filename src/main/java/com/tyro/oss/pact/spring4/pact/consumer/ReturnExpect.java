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

import static com.tyro.oss.pact.spring4.util.JsonSchemaMatcher.matchesSchema;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.ComparisonFailure;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriUtils;


import com.google.common.base.Function;
import com.google.common.collect.Maps;

import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring4.pact.model.Pact;
import com.tyro.oss.pact.spring4.util.ObjectStringConverter;

public class ReturnExpect<T> {

    private final RestRequestDescriptor restRequestDescriptor;
    private final File pactFile;
    private final Boolean withoutRecord;
    private final String workflowId;
    private final List<Pact.ProviderState> providerStates;
    private final MockRestServiceServer mockRestServiceServer;
    private final ObjectStringConverter objectConverter;
    private int times = 1;
    private final Map<String, Matcher<? super List<String>>> additionalExpectedHeaders = new HashMap<>();
    private MediaType contentType;
    private String schema;

    public ReturnExpect(RestRequestDescriptor restRequestDescriptor, File pactFile, String workflowId, List<Pact.ProviderState> providerStates, Boolean withoutRecord, MockRestServiceServer mockRestServiceServer, ObjectStringConverter objectConverter, MediaType contentType) {
        this.restRequestDescriptor = restRequestDescriptor;
        this.pactFile = pactFile;
        this.withoutRecord = withoutRecord;
        this.workflowId = workflowId;
        this.providerStates = providerStates;
        this.mockRestServiceServer = mockRestServiceServer;
        this.objectConverter = objectConverter;
        this.contentType = contentType;
    }

    /**
     * Expect this interaction to occur multiple times.
     *
     * @param times
     * @return
     */
    public ReturnExpect<T> times(final int times) {
        this.times = times;
        return this;
    }

    /**
     * Expect a particular header to be sent.
     *
     * @param headerName
     * @param headerValue
     * @return
     */
    public ReturnExpect<T> andExpectHeader(String headerName, String headerValue) {
        additionalExpectedHeaders.put(headerName, is(asList(headerValue)));
        return this;
    }

    /**
     * Expect a header to be sent whose value satisfies a Hamcrest matcher.
     *
     * @param headerName
     * @param matcher
     * @return
     */
    public ReturnExpect<T> andExpectHeader(String headerName, Matcher<? super List<String>> matcher) {
        additionalExpectedHeaders.put(headerName, matcher);
        return this;
    }

    /**
     * Record this expectation and expect no return value. This implies a 200 OK response with an empty response body.
     */
    public void andReturn() {
        createRequestExpectation(ResponseEntity.ok().build(), null);
    }

    /**
     * Record this expectation and expect a value to be returned. This implies a 200 OK reponse status.
     *
     * @param value The object representation of the response that will be converted by the supplied {@link ObjectStringConverter}
     *              and compared with the actual response.
     */
    public void andReturn(final T value) {
        andReturn(ResponseEntity.ok().body(value));
    }

    /**
     * Record this expectation and expect a customised response entity.
     *
     * @param value ResponseEntity containing the HTTP status and body we expect to be returned.
     */
    public void andReturn(final ResponseEntity<T> value) {
        Class restRequestDescriptorResponseType = restRequestDescriptor.getResponseType();
        Type type = restRequestDescriptorResponseType == null ? restRequestDescriptor.getParameterizedResponseType().getType() : restRequestDescriptorResponseType;
        createRequestExpectation(value, type);
    }

    /**
     * Record this expectation and expect a response that overrides the type previously defined.
     *
     * @param value The object representation of the response that will be converted by the supplied {@link ObjectStringConverter}
     *              and compared with the actual response.
     * @param type
     */
    public void andReturn(final T value, Type type) {
        createRequestExpectation(ResponseEntity.ok().body(value), type);
    }

    /**
     * Record this expectation and expect an error response that overrides the type previously defined.
     *
     * @param errorValue
     */
    public void andError(ResponseEntity errorValue) {
        createRequestExpectation(errorValue, errorValue.getBody() == null ? null : errorValue.getBody().getClass());
    }

    /**
     * Inform the provider to ignore all other expectations and match only against the schema provided for this workflow.
     *
     * @param schema A string representation of the schema
     * @return
     */
    public ReturnExpect<T> matchingSchema(String schema) {
        this.schema = schema;
        return this;
    }

    /**
     * Inform the provider to ignore all other expectations and match only against the schema provided for this workflow.
     *
     * @param inputStream An InputStream representation of the schema
     * @return
     */
    public ReturnExpect<T> matchingSchema(InputStream inputStream) {
        try {
            return matchingSchema(IOUtils.toString(inputStream, "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createRequestExpectation(final ResponseEntity entity, final Type type) {
        for (int i = 1; i <= times; i++) {
            String bodyContent = extractBodyContent(entity, type);

            if (StringUtils.isNotBlank(schema)) {
                assertThat(bodyContent, matchesSchema(schema));
            }

            mockRestServiceServer.expect(clientRequest -> {
                MockClientHttpRequest mockRequest = (MockClientHttpRequest) clientRequest;
                assertThat(mockRequest.getURI().toString(), containsString(urlencode(restRequestDescriptor.getUrl())));
                assertThat(mockRequest.getMethod(), is(restRequestDescriptor.getMethod()));

                MediaType contentType1 = mockRequest.getHeaders().getContentType();
                String actualBody = mockRequest.getBodyAsString();
                String expectedBody = extractBodyContent(restRequestDescriptor.getRequest());
                if (contentType1 != null && "json".equalsIgnoreCase(contentType1.getSubtype())) {
                    try {
                        JSONAssert.assertEquals(expectedBody, actualBody, false);
                    } catch (JSONException e) {
                        try {
                            assertThat(actualBody, is(expectedBody));
                        } catch (AssertionError ex) {
                            throw new ComparisonFailure(e.getMessage(), expectedBody, actualBody);
                        }
                    }
                } else {
                    assertThat(actualBody, is(expectedBody));
                }

                assertRequestHeaders(mockRequest.getHeaders(), restRequestDescriptor.getRequest(), additionalExpectedHeaders);
            }).andRespond(clientRequest -> {
                ClientHttpResponse response = MockRestResponseCreators
                        .withStatus(entity.getStatusCode())
                        .headers(entity.getHeaders())
                        .contentType(getContentType(entity))
                        .body(bodyContent).createResponse(clientRequest);
                if (!withoutRecord) {
                    try {
                        Pact pact = getPactFile();
                        getOrCreateWorkflowAndAddInteraction(pact, clientRequest, response);
                        Pact.writePact(pact, pactFile, objectConverter);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return response;
            });
        }
    }

    private MediaType getContentType(ResponseEntity response) {
        if (response.getBody() == null) {
            return null;
        }

        if (response.getHeaders() == null || response.getHeaders().getContentType() == null) {
            return contentType;
        }

        return response.getHeaders().getContentType();
    }

    private void assertRequestHeaders(HttpHeaders actualHeaders, Object requestObject, Map<String, Matcher<? super List<String>>> additionalExpectedHeaders) {
        Map<String, Matcher<? super List<String>>> expectedHeaders = new HashMap<>();

        if (requestObject != null && requestObject instanceof HttpEntity) {
            HttpEntity httpEntity = (HttpEntity) requestObject;
            HttpHeaders headers = httpEntity.getHeaders();
            Map<String, Matcher<List<String>>> stringMatcherMap = Maps.transformValues(headers, new Function<List<String>, Matcher<List<String>>>() {
                @Override
                public Matcher<List<String>> apply(List<String> input) {
                    return is(input);
                }
            });
            expectedHeaders.putAll(stringMatcherMap);
        }

        expectedHeaders.putAll(additionalExpectedHeaders);

        Set<String> headerNames = expectedHeaders.keySet();
        for (String headerName : headerNames) {
            Matcher<? super List<String>> headerValuesMatcher = expectedHeaders.get(headerName);
            assertThat(format("Contains header %s", headerName), actualHeaders.containsKey(headerName), is(true));
            assertThat(format("'%s' header value fails assertion", headerName), actualHeaders.get(headerName), headerValuesMatcher);
        }
    }

    private Pact getPactFile() throws IOException {
        Pact pact;
        if (pactFile.exists() && (pactFile.length() > 0)) {
            pact = Pact.parse(FileUtils.readFileToString(pactFile), objectConverter);
        } else {
            pact = Pact.newPact(objectConverter);
        }
        return pact;
    }

    private void getOrCreateWorkflowAndAddInteraction(Pact pact, ClientHttpRequest clientRequest, ClientHttpResponse response) throws IOException {
        String bodyString = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        response.getBody().reset();

        Pact.Interaction interaction = new Pact.Interaction(
                null,
                new Pact.InteractionRequest(restRequestDescriptor.getMethod(), urlencode(restRequestDescriptor.getUrl()), clientRequest.getHeaders(), extractBodyContent(restRequestDescriptor.getRequest())),
                new Pact.InteractionResponse(response.getRawStatusCode(), response.getHeaders(), bodyString, schema),
                objectConverter);
        Pact.Workflow workflow = pact.getWorkflow(this.workflowId, this.providerStates);
        workflow.addInteraction(interaction);
    }

    private String extractBodyContent(Object entity) {
        return extractBodyContent(entity, null);
    }

    private String extractBodyContent(Object entity, Type type) {
        Object bodyContent = (entity instanceof HttpEntity) ? ((HttpEntity) entity).getBody() : entity;

        if (bodyContent == null) {
            return "";
        }

        if (bodyContent instanceof String) {
            return (String) bodyContent;
        }

        return type == null ? objectConverter.fromObject(bodyContent) : objectConverter.fromObject(bodyContent, type);
    }

    private static String urlencode(String path) {
        try {
            return UriUtils.encodeQuery(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
