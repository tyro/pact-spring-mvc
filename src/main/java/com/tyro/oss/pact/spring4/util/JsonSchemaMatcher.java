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
/*
 * Copyright (c) 2003 - 2016 Tyro Payments Limited.
 * Lv1, 155 Clarence St, Sydney NSW 2000.
 * All rights reserved.
 */
package com.tyro.oss.pact.spring4.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class JsonSchemaMatcher extends TypeSafeDiagnosingMatcher<String> {

    public static JsonSchemaMatcher matchesSchema(String schema) {
        return new JsonSchemaMatcher(schema);
    }

    private JsonSchema schema;
    private String unparsedSchema;

    private JsonSchemaMatcher(String unparsedSchema) {
        this.unparsedSchema = unparsedSchema;
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance();
        this.schema = jsonSchemaFactory.getSchema(unparsedSchema);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches a json schema of " + unparsedSchema);
    }

    @Override
    protected boolean matchesSafely(String item, Description mismatchDescription) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = getJsonNode(item, objectMapper);

        List<String> validationMessages = schema.validate(jsonNode)
                .stream()
                .map(ValidationMessage::getMessage)
                .collect(toList());

        mismatchDescription.appendValueList("schema validation failed with the following errors: \n - ", "\n - ", "", validationMessages);

        return validationMessages.isEmpty();
    }

    private JsonNode getJsonNode(String content, ObjectMapper objectMapper) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return jsonNode;
    }
}
