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
package com.tyro.oss.pact.spring.pact.consumer;

import com.tyro.oss.pact.spring.pact.PactBrokerUrlSource;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class PactPublisher {

    private PactPublisher() {
    }

    public static void main(String[] args) throws Exception {
        String consumer = args[0];
        String version = args[1];
        String pactFileRoot = args[2];

        if (args.length != 3) {
            throw new IllegalArgumentException("publish-pact profile in the pom.xml should contain the following arguments <CONSUMER> <VERSION> <PACT-FILE-LOCATION>");
        }

        PactBrokerUrlSource pactBrokerUrlSource = new PactBrokerUrlSource();
        publishPactFiles(consumer, version, pactFileRoot, pactBrokerUrlSource.getPactUrlForPublish());
    }

    public static void publishPactFiles(String consumer, String version, String pactFileRoot, String publishUrl) throws IOException {
        for (File pactFile : findPactFiles(pactFileRoot)) {
            String provider = pactFile.getName().replace("_pacts.json", "").replace("_", "-");
            publishPactFile(provider, consumer, version, pactFile.getAbsolutePath(), publishUrl);
        }
    }

    private static File[] findPactFiles(String pactFileRoot) {
        File pactFileRootDirectory = new File(pactFileRoot);
        if (!pactFileRootDirectory.exists() || !pactFileRootDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("Pact file root directory either does not exist, or is not a directory: %s", pactFileRoot));
        }

        return pactFileRootDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("_pacts.json");
            }
        });
    }

    private static void publishPactFile(String provider, String consumer, String version, String pactFile, String brokerBaseUrl) throws IOException {
        if (version.contains("-SNAPSHOT")) {
            version = version.replace("-SNAPSHOT", "");
        }

        String url = brokerBaseUrl
                + "/pacts/provider/"
                + provider
                + "/consumer/"
                + consumer
                + "/version/"
                + version;

        String pactJson = FileUtils.readFileToString(new File(pactFile), UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(pactJson, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(url, httpEntity);
        } catch (ResourceAccessException e) {
            throw new NoPactBrokerException(String.format("Could not reach Pact broker: %s.", brokerBaseUrl), e);
        } catch (HttpClientErrorException e) {
            throw e;
        }
    }
}
