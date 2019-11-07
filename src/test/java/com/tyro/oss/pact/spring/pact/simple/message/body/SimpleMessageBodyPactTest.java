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
package com.tyro.oss.pact.spring.pact.simple.message.body;

import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring.pact.consumer.TuPactRecordingServer;
import com.tyro.oss.pact.spring.pact.simple.message.body.service.BooleanService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SimpleMessageBodyPactTest {

    private TuPactRecordingServer recordingServer;
    private BooleanService booleanService;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();

        booleanService = new BooleanService(restTemplate);

        File pactFile = new File("target/pact/example_provider_pacts.json");
        recordingServer = TuPactRecordingServer.createServer(restTemplate, pactFile);
    }

    @Test
    void shouldRetrieveDefaultValue(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer.expect(new RestRequestDescriptor<>("/string", HttpMethod.PUT, Boolean.TRUE, String.class))
                .andReturn("Blah");

        assertThat(booleanService.getString(), is("Blah"));
    }

    @AfterEach
    void closeTuPact() {
        recordingServer.close();
    }
}
