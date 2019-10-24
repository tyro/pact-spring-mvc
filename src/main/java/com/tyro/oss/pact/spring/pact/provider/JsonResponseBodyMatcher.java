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
package com.tyro.oss.pact.spring.pact.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tyro.oss.pact.spring.pact.model.Pact;
import org.apache.commons.lang3.StringUtils;
import org.junit.ComparisonFailure;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.tyro.oss.pact.spring.util.JsonSchemaMatcher.matchesSchema;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;

public class JsonResponseBodyMatcher implements ResponseBodyMatcher {

    @Override
    public boolean canHandle(Pact.InteractionResponse interactionResponse) {
        return APPLICATION_JSON.equals(interactionResponse.getHeaders().getContentType());
    }

    @Override
    public void assertContent(ResultActions expectations, Pact.InteractionResponse response) throws Exception {
        if (StringUtils.isNotEmpty(response.getSchema())) {
            expectations.andExpect(new JsonSchemaResultMatcher(response.getSchema()));
        } else {
            expectations.andExpect(new JsonResultMatcher(response.getBody()));
        }
    }

    private static class JsonSchemaResultMatcher implements ResultMatcher {
        private String schema;

        JsonSchemaResultMatcher(String schema) {
            this.schema = schema;
        }

        @Override
        public void match(MvcResult result) throws Exception {
            String content = result.getResponse().getContentAsString();

            assertThat(content, matchesSchema(schema));
        }
    }

    private static class JsonResultMatcher implements ResultMatcher {

        private final String expectedJsonResponse;

        JsonResultMatcher(String expectedJsonResponse) {
            this.expectedJsonResponse = expectedJsonResponse;
        }

        @Override
        public void match(MvcResult result) throws Exception {
            String content = result.getResponse().getContentAsString();

            final JsonParser parser = new JsonParser();
            final JsonElement actual = parser.parse(content);

            if (actual.isJsonPrimitive()) {
                final JsonElement expected = parser.parse(expectedJsonResponse);
                assertThat(actual, is(expected));
            } else {
                try {
                    JSONAssert.assertEquals(expectedJsonResponse, content, false);
                } catch (AssertionError e) {
                    throw new ComparisonFailure(e.getMessage(), expectedJsonResponse, content);
                }
            }
        }
    }
}
