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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tyro.oss.pact.spring4.pact.provider.ConsumerFilter;

public class PactBrokerUrlSource {

    private static final Logger LOG = LoggerFactory.getLogger(PactBrokerUrlSource.class);

    public static final String PACT_BROKER_PROPERTIES_FILE = "pact-broker.properties";
    public static final String PACT_BROKER_PUBLISH_URL_PROPERTY = "pact.broker.publish.url";
    public static final String PACT_BROKER_DOWNLOAD_URLS_PROPERTY = "pact.broker.download.urls";

    private Properties properties = new Properties();

    public PactBrokerUrlSource() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream propertiesFile = classLoader.getResourceAsStream(PACT_BROKER_PROPERTIES_FILE);
        try {
            if (propertiesFile != null) {
                properties.load(propertiesFile);
            }
        } catch (IOException e) {
            LOG.debug("No broker.properties properties file found on classpath... will try system properties");
        }
    }

    public String getPactUrlForPublish() {
        return getProperty(PACT_BROKER_PUBLISH_URL_PROPERTY, "pact publish URL");
    }

    public String[] getPactUrlsToDownloadPacts() {
        return getProperty(PACT_BROKER_DOWNLOAD_URLS_PROPERTY, "pact download URLs").split(",");
    }

    private String getProperty(String property, String description) {
        String propertyValue = System.getProperty(property);
        if (propertyValue == null) {
            propertyValue = properties.getProperty(property);
        }
        if (propertyValue == null) {
            throw new IllegalStateException("Failed to read " + description + ". Use either broker.properties or System properties to define:" + property);
        }
        return propertyValue;
    }
}
