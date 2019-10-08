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
package com.tyro.oss.pact.spring4.pact.provider;

import com.tyro.oss.pact.spring4.pact.PactBrokerUrlSource;
import com.tyro.oss.pact.spring4.pact.model.Pact;
import com.tyro.oss.pact.spring4.pact.provider.PactTestRunner.PactDefinition;
import com.tyro.oss.pact.spring4.util.ObjectStringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class DefaultPactResolver implements PactResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPactResolver.class);

    public static final int PACT_DOWNLOAD_TIMEOUT_MILLIS = 5000;

    @Override
    public List<Pact> resolvePacts(final PactDefinition pactDefinition, final ObjectStringConverter jsonConverter) throws Exception {

        if (!StringUtils.isEmpty(pactDefinition.localPactFilePath())) {
            String pactJson = loadPactFile(pactDefinition.localPactFilePath());
            Pact pact = Pact.parse(pactJson, jsonConverter);

            pact.setDisplayVersion("local");
            pact.setDisplayName(pactDefinition.consumer() + "-local");

            return singletonList(pact);
        }

        return getPactVersionsToRun(pactDefinition).stream()
                .map(version -> resolvePact(pactDefinition, version, jsonConverter))
                .collect(toList());
    }

    protected Pact resolvePact(PactDefinition pactDefinition, String version, ObjectStringConverter jsonConverter) {
        ResponseEntity<String> response = downloadPact(pactDefinition, version);

        if (response == null) {
            throw new IllegalStateException("Unable to successfully resolve pact file from any broker");
        }

        String pactJson = response.getBody();

        Pact pact;

        try {
            pact = Pact.parse(pactJson, jsonConverter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (response.getHeaders().get("X-Pact-Consumer-Version") != null) {
            pact.setNumericVersion(response.getHeaders().get("X-Pact-Consumer-Version").get(0));
        } else {
            pact.setNumericVersion(version);
        }
        pact.setDisplayVersion(version);
        pact.setDisplayName(pactDefinition.consumer() + "-" + version + response.getHeaders().get("X-Pact-Consumer-Version"));

        return pact;
    }

    protected ResponseEntity<String> downloadPact(PactDefinition pactDefinition, String version) {
        PactBrokerUrlSource pactBrokerUrlSource = new PactBrokerUrlSource();

        for (String eachUrl : pactBrokerUrlSource.getPactUrlsToDownloadPacts()) {
            try {
                LOG.info(String.format("Downloading pact [%s] from broker at: %s", version, eachUrl));
                return createRestTemplate().getForEntity(pactUrl(pactDefinition, version, eachUrl), String.class);
            } catch (RestClientException e) {
                LOG.warn(String.format("Couldn't download pact [%s] from url: %s", version, eachUrl), e);
            }
        }

        return null;
    }

    protected RestTemplate createRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(PACT_DOWNLOAD_TIMEOUT_MILLIS);
        factory.setConnectTimeout(PACT_DOWNLOAD_TIMEOUT_MILLIS);

        return new RestTemplate(factory);
    }

    protected String loadPactFile(String localPactFilePath) {
        try {
            return FileUtils.readFileToString(new File(localPactFilePath), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Pact file from local file path: " + localPactFilePath, e);
        }
    }

    protected String pactUrl(PactDefinition pactDefinition, String version, String brokerUrl) {
        return brokerUrl
                + "/pacts/provider/"
                + pactDefinition.provider()
                + "/consumer/"
                + pactDefinition.consumer()
                + (version.startsWith("latest") ? "/" + version : "/version/" + version);
    }

    protected List<String> getPactVersionsToRun(PactDefinition pactDefinition) {
        String[] pactVersionsFromAnnotation = pactDefinition.pactVersions();

        if (pactVersionsFromAnnotation != null && pactVersionsFromAnnotation.length > 0) {
            return asList(pactVersionsFromAnnotation);
        } else {
            return getDefaultPactVersions(pactDefinition);
        }
    }

    protected List<String> getDefaultPactVersions(PactDefinition pactDefinition) {
        return singletonList("latest");
    }
}
