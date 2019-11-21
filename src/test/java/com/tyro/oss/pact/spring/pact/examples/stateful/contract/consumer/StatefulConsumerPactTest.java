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
package com.tyro.oss.pact.spring.pact.examples.stateful.contract.consumer;

import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring.pact.consumer.TuPactRecordingServer;
import com.tyro.oss.pact.spring.pact.examples.stateful.contract.api.BookCollectionDTO;
import com.tyro.oss.pact.spring.pact.examples.stateful.contract.api.BookDTO;
import com.tyro.oss.pact.spring.pact.examples.stateful.contract.consumer.service.BookshelfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class StatefulConsumerPactTest {

    private TuPactRecordingServer recordingServer;
    private BookshelfService bookshelfService;

    private BookDTO shelvedRead;
    private BookDTO shelvedUnread;
    private BookDTO unshelvedUnread;
    private BookDTO unshelvedRead;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();

        bookshelfService = new BookshelfService(restTemplate);

        File pactFile = new File("target/pact/stateful_contract_pacts.json");

        shelvedRead = new BookDTO("Essentialism: The Disciplined Pursuite of Less", true, true);
        shelvedUnread = new BookDTO("Quiet: The Power of Introverts in a World That Can't Stop Talking", true, false);
        unshelvedRead = new BookDTO("Harry Potter and the Order of the Phoenix", false, true);
        unshelvedUnread = new BookDTO("The Winds of Winter", false, false);

        recordingServer = TuPactRecordingServer.createServer(restTemplate, pactFile)
                .inState("withKnownBook", shelvedRead)
                .inState("withKnownBook", shelvedUnread)
                .inState("withKnownBook", unshelvedRead)
                .inState("withKnownBook", unshelvedUnread);
    }

    @Test
    void shouldRetrieveEntireBookshelf(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer
                .expect(getBooksOwnedDescriptor())
                .andReturn(new BookCollectionDTO(asList(shelvedRead, shelvedUnread)));

        assertThat(bookshelfService.getBooksOwned(), is(new BookCollectionDTO(shelvedRead, shelvedUnread)));
    }

    @Test
    void shouldRetrieveEntireReadList(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer
                .expect(getBooksReadDescriptor())
                .andReturn(new BookCollectionDTO(asList(shelvedRead, unshelvedRead)));

        assertThat(bookshelfService.getBooksRead(), is(new BookCollectionDTO(shelvedRead, unshelvedRead)));
    }

    @AfterEach
    void closeTuPact() {
        recordingServer.close();
    }

    private RestRequestDescriptor<BookCollectionDTO> getBooksOwnedDescriptor() {
        return new RestRequestDescriptor<>("/shelf", HttpMethod.GET, null, BookCollectionDTO.class);
    }

    private RestRequestDescriptor<BookCollectionDTO> getBooksReadDescriptor() {
        return new RestRequestDescriptor<>("/read", HttpMethod.GET, null, BookCollectionDTO.class);
    }
}
