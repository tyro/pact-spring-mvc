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
package com.tyro.oss.pact.spring4.pact.simple.message.body;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


import java.io.File;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;


import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring4.pact.consumer.TuPactRecordingServer;
import com.tyro.oss.pact.spring4.pact.simple.message.body.service.BooleanService;

public class SimpleMessageBodyPactTest {

    @Rule
    public TestName testName = new TestName();
    private TuPactRecordingServer recordingServer;
    private BooleanService booleanService;

    @Before
    public void setup() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        booleanService = new BooleanService(restTemplate);

        File pactFile = new File("target/pact/example_provider_pacts.json");
        recordingServer = TuPactRecordingServer.createServer(restTemplate, pactFile);
    }

    @Test
    public void shouldRetrieveDefaultValue() throws Exception {
        recordingServer.startWorkflow(testName.getMethodName());

        recordingServer.expect(new RestRequestDescriptor<>("/string", HttpMethod.PUT, Boolean.TRUE, String.class))
                .andReturn("Blah");

        assertThat(booleanService.getString(), is("Blah"));
    }

    @After
    public void closeTuPact() throws Exception {
        recordingServer.close();
    }
}
