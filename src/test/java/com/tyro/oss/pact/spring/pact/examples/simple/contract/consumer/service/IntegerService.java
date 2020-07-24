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
package com.tyro.oss.pact.spring.pact.examples.simple.contract.consumer.service;

import com.tyro.oss.pact.rest.RestRequestDescriptor;
import com.tyro.oss.pact.spring.pact.examples.RestRequestInvoker;
import com.tyro.oss.pact.spring.pact.examples.simple.contract.api.IntegerDTO;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class IntegerService {

    private RestRequestInvoker restRequestInvoker;

    public IntegerService(RestTemplate restTemplate) {
        this.restRequestInvoker = new RestRequestInvoker(restTemplate);
    }

    public IntegerDTO getLatestInteger() {
        return restRequestInvoker.forDescriptor(new RestRequestDescriptor<>("/integer", HttpMethod.GET, null, IntegerDTO.class));
    }
}
