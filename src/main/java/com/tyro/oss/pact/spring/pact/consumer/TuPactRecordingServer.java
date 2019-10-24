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
package com.tyro.oss.pact.spring.pact.consumer;

import com.google.gson.GsonBuilder;
import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring.pact.model.Pact.ProviderState;
import com.tyro.oss.pact.spring.util.GsonStringConverter;
import com.tyro.oss.pact.spring.util.ObjectStringConverter;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TuPactRecordingServer {

    private final File pactFile;
    private final ObjectStringConverter objectConverter;
    private final RestTemplate restTemplate;
    private final ClientHttpRequestFactory originalRequestFactory;
    private final MediaType contentType;

    private String workflowId;
    private final List<ProviderState> providerStates = new ArrayList<>();
    private MockRestServiceServer mockRestServiceServer;

    private boolean withoutRecording = false;

    protected TuPactRecordingServer(RestTemplate restTemplate, File pactFile, ObjectStringConverter objectConverter, MediaType contentType) {
        this.restTemplate = restTemplate;
        this.pactFile = pactFile;
        this.objectConverter = objectConverter;
        originalRequestFactory = restTemplate.getRequestFactory();
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
        this.contentType = contentType;
    }

    public static TuPactRecordingServer createServer(RestTemplate restTemplate, File pactFile) {
        return new TuPactRecordingServer(restTemplate, pactFile, new GsonStringConverter(new GsonBuilder().create()), MediaType.APPLICATION_JSON);
    }

    public static TuPactRecordingServer createServer(RestTemplate restTemplate, File pactFile, ObjectStringConverter objectConverter) {
        return new TuPactRecordingServer(restTemplate, pactFile, objectConverter, MediaType.APPLICATION_JSON);
    }

    public static TuPactRecordingServer createServer(RestTemplate restTemplate, File pactFile, ObjectStringConverter objectConverter, MediaType contentType) {
        return new TuPactRecordingServer(restTemplate, pactFile, objectConverter, contentType);
    }

    /**
     * Assign a unique identifier to this workflow. Must be unique per provider and consumer pair.
     *
     * @param id A unique identifier
     */
    public void startWorkflow(String id) {
        this.workflowId = id;
    }

    /**
     * Request for the provider to be in a particular state before the next interaction.
     *
     * @param description The name of the state method that is defined in the provider test by @{@link com.tyro.oss.pact.spring.pact.provider.annotations.ProviderState}
     * @param states      Parameters that will be passed to the method identified by the description
     * @return The same recording instance with the provider state recorded.
     */
    public TuPactRecordingServer inState(String description, Object... states) {
        providerStates.add(new ProviderState(description, objectConverter, states));
        return this;
    }

    /**
     * Prepare an expection for a REST request that will be called on the server. The expection will not be recorded until .andReturn(...) is called.
     * A workflow identifier must be specified before calling this method. See startWorkflow(...).
     *
     * @param descriptor A descriptor of the REST request that we expect.
     * @return A follow-on interface that takes the expected return value and records the interaction.
     */
    public <T> ReturnExpect<T> expect(RestRequestDescriptor<T> descriptor) {
        if (workflowId == null) {
            throw new IllegalStateException("Workflow Id has not been specified. Try calling startWorkflow(...) with JUnit's @Rule TestName in an @Before method.");
        }

        boolean withoutRecordingThisTime = this.withoutRecording;
        this.withoutRecording = false;
        return new ReturnExpect<>(descriptor, pactFile, workflowId, providerStates, withoutRecordingThisTime, mockRestServiceServer, objectConverter, contentType);
    }

    /**
     * Do not record this interaction but still return the stub response.
     *
     * @return The same recording instance with recording disabled.
     */
    public TuPactRecordingServer withoutRecording() {
        withoutRecording = true;
        return this;
    }

    /**
     * Verify all expectations were fulfilled and remove them.
     */
    public void reset() {
        this.mockRestServiceServer.verify();
        this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * Verify all expectations were fulfilled and restore the RestTemplate to its original state.
     */
    public void close() {
        this.mockRestServiceServer.verify();
        this.restTemplate.setRequestFactory(originalRequestFactory);
    }
}
