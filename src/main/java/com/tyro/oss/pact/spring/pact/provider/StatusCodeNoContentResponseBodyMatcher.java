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
package com.tyro.oss.pact.spring.pact.provider;

import com.tyro.oss.pact.spring.pact.model.Pact;
import org.apache.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusCodeNoContentResponseBodyMatcher implements ResponseBodyMatcher {

    @Override
    public boolean canHandle(Pact.InteractionResponse interactionResponse) {
        return interactionResponse.getStatus() == HttpStatus.SC_NO_CONTENT;
    }

    @Override
    public void assertContent(ResultActions expectations, Pact.InteractionResponse response) throws Exception {
        String responseContent = expectations.andReturn().getResponse().getContentAsString();
        assertEquals("", responseContent, "204 HTTP Status Codes must not contain a response body");
    }

}
