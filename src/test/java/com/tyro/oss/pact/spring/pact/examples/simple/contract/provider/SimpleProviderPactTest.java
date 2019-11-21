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
package com.tyro.oss.pact.spring.pact.examples.simple.contract.provider;

import com.tyro.oss.pact.spring.pact.provider.PactTest;
import com.tyro.oss.pact.spring.pact.provider.annotations.PactDefinition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@PactDefinition(
        provider = "example-provider",
        consumer = "example-consumer",
        localPactFilePath = "target/pact/example_provider_pacts.json"
)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SimpleWebConfig.class)
@WebAppConfiguration
public class SimpleProviderPactTest extends PactTest {

    @Override
    protected String getServletContextPathWithoutTrailingSlash() {
        return "/servletContextPath";
    }
}
