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
package com.tyro.oss.pact.spring4.pact;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PactBrokerUrlPropertiesTest {

    private PactBrokerUrlSource pactBrokerUrlSource;

    private String oldBrokerDownloadUrlProperty;
    private String oldBrokerPublishUrlProperty;

    @Before
    public void setpURLSource() throws Exception {
        pactBrokerUrlSource = new PactBrokerUrlSource();
        oldBrokerDownloadUrlProperty = System.getProperty(PactBrokerUrlSource.PACT_BROKER_DOWNLOAD_URLS_PROPERTY);
        oldBrokerPublishUrlProperty = System.getProperty(PactBrokerUrlSource.PACT_BROKER_PUBLISH_URL_PROPERTY);
    }

    @After
    public void returnSystemProperty() {
        if (oldBrokerDownloadUrlProperty != null) {
            System.setProperty(PactBrokerUrlSource.PACT_BROKER_DOWNLOAD_URLS_PROPERTY, oldBrokerDownloadUrlProperty);
        }
        if (oldBrokerPublishUrlProperty != null) {
            System.setProperty(PactBrokerUrlSource.PACT_BROKER_PUBLISH_URL_PROPERTY, oldBrokerPublishUrlProperty);
        }
    }

    @Test
    public void shouldGetBrokerPublishURLsWhenSystemPropertyIsNotDefined() {
        System.clearProperty(PactBrokerUrlSource.PACT_BROKER_PUBLISH_URL_PROPERTY);
        assertThat(pactBrokerUrlSource.getPactUrlForPublish(), is("test"));
    }

    @Test
    public void shouldGetMultipleDownloadURLsWhenSystemPropertyIsNotDefined() throws Exception {
        System.clearProperty(PactBrokerUrlSource.PACT_BROKER_DOWNLOAD_URLS_PROPERTY);
        String[] propertyValues = new String[]{"test1", "test2"};
        assertThat(pactBrokerUrlSource.getPactUrlsToDownloadPacts(), arrayContaining(propertyValues));
    }
}
