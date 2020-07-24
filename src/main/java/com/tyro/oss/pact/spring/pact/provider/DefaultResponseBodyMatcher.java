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

import com.tyro.oss.pact.spring.pact.model.Pact;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class DefaultResponseBodyMatcher implements ResponseBodyMatcher {

    @Override
    public boolean canHandle(Pact.InteractionResponse interactionResponse) {
        return true;
    }

    @Override
    public void assertContent(ResultActions expectations, Pact.InteractionResponse response) throws Exception {
        expectations.andExpect(content().string(response.getBody()));
    }
}
