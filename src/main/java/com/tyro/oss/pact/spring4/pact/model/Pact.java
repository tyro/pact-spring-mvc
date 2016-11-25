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
package com.tyro.oss.pact.spring4.pact.model;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.tyro.oss.pact.spring4.util.ObjectStringConverter;

public class Pact {


    @Deprecated
    private List<Interaction> interactions;

    private transient ObjectStringConverter internalJsonConverter;

    private transient String numericVersion;

    private transient String displayVersion;

    private transient String displayName;

    private final Map<String, Workflow> workFlows = new HashMap<>();

    public static Pact newPact(ObjectStringConverter jsonConverter) {
        return new Pact(new ArrayList<Interaction>(), jsonConverter);
    }

    public static Pact parse(String json, ObjectStringConverter jsonConverter) throws IOException {
        Pact pact = jsonConverter.fromString(json, Pact.class);
        pact.internalJsonConverter = jsonConverter;

        for (Interaction interaction : pact.getInteractions()) {
            interaction.setJsonConverter(jsonConverter);
        }
        for (Map.Entry<String, Workflow> stringWorkflowEntry : pact.getWorkFlows().entrySet()) {
            for (Interaction interaction : stringWorkflowEntry.getValue().getInteractions()) {
                interaction.setJsonConverter(jsonConverter);
            }
        }
        return pact;
    }

    public static void writePact(Pact pact, File pactFile, ObjectStringConverter jsonConverter) throws IOException {
        FileUtils.writeStringToFile(pactFile, jsonConverter.fromObject(pact));
    }

    private Pact(List<Interaction> interactions, ObjectStringConverter jsonConverter) {
        this.interactions = interactions;
        this.internalJsonConverter = jsonConverter;
        if (interactions != null) {
            for (Interaction interaction : interactions) {
                interaction.setJsonConverter(jsonConverter);
            }
        }

    }

    public List<Interaction> getInteractions() {
        if (interactions != null) {
            return Collections.unmodifiableList(interactions);
        }
        return Collections.EMPTY_LIST;
    }

    public void addInteraction(Interaction interaction, String interactionName) {
        if (StringUtils.isEmpty(interactionName)) {
            interactionName = String.valueOf(interactions.size());
        }
        interaction.setId(interactionName);
        interactions.add(interaction);
    }

    public Workflow getWorkflow(String id, List<ProviderState> providerStates) {
        if (workFlows.containsKey(id)) {
            return workFlows.get(id);
        } else {
            Workflow workflow = new Workflow(id, providerStates, internalJsonConverter);
            workFlows.put(id, workflow);
            return workflow;
        }
    }

    public Map<String, Workflow> getWorkFlows() {
        return workFlows;
    }

    public String getNumericVersion() {
        return numericVersion;
    }

    public void setNumericVersion(String numericVersion) {
        this.numericVersion = numericVersion;
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    public void setDisplayVersion(String displayVersion) {
        this.displayVersion = displayVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static class Workflow {

        private String id;

        private List<ProviderState> providerStates;

        private final List<Interaction> interactions = new ArrayList<>();

        private transient ObjectStringConverter jsonConverter;

        public Workflow(String id, List<ProviderState> providerStates, ObjectStringConverter jsonConverter) {
            this.id = id;
            this.providerStates = providerStates;
            this.jsonConverter = jsonConverter;
        }

        public List<Interaction> getInteractions() {
            return interactions;
        }

        public void addInteraction(Interaction interaction) {
            interaction.setJsonConverter(jsonConverter);
            interactions.add(interaction);
        }

        public String getId() {
            return id;
        }

        public List<ProviderState> getProviderStates() {
            return providerStates;
        }

        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other, "id");
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this, "id");
        }
    }

    public static class Interaction {

        private String id;

        @Deprecated
        private ProviderState providerState;

        private InteractionRequest request;

        private InteractionResponse response;

        public void setJsonConverter(ObjectStringConverter jsonConverter) {
            this.jsonConverter = jsonConverter;
        }

        private transient ObjectStringConverter jsonConverter;

        public Interaction(ProviderState providerState, InteractionRequest request, InteractionResponse response, ObjectStringConverter jsonConverter) {
            this.providerState = providerState;
            this.request = request;
            this.response = response;
            this.jsonConverter = jsonConverter;
        }

        public String getId() {
            return id;
        }

        public ProviderState getProviderState() {
            return providerState;
        }

        public InteractionRequest getRequest() {
            return request;
        }

        public InteractionResponse getResponse() {
            return response;
        }

        private void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return jsonConverter.fromObject(this);
        }

        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other, "id");
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this, "id");
        }
    }

    public static class InteractionRequest {

        private HttpMethod method;

        private String uri;

        private HttpHeaders headers;

        private String body;

        public InteractionRequest(HttpMethod method, String uri, HttpHeaders headers, String body) {
            this.method = method;
            this.uri = uri;
            this.headers = headers;
            this.body = body;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }


        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    public static class InteractionResponse {

        private int status;

        private HttpHeaders headers;

        private String body;

        private String schema;

        public InteractionResponse(int status, HttpHeaders headers, String body, String schema) {
            this.status = status;
            this.headers = headers;
            this.body = body;
            this.schema = schema;
        }

        public int getStatus() {
            return status;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        public String getSchema() {
            return schema;
        }

        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    public static class ProviderState {

        private String description;

        public void setJsonConverter(ObjectStringConverter jsonConverter) {
            this.jsonConverter = jsonConverter;
        }

        private transient ObjectStringConverter jsonConverter;

        private List<ProviderArgument> providerArguments = new ArrayList<>();

        public ProviderState(String description, ObjectStringConverter jsonConverter, Object... states) {
            this.description = description;
            this.jsonConverter = jsonConverter;
            for (Object state : states) {
                providerArguments.add(new ProviderArgument(jsonConverter.fromObject(state), state.getClass().getName()));
            }
        }

        public String getDescription() {
            return description;
        }

        public Object[] getStates(Type[] genericParameterTypes) {
            List<Object> states = new ArrayList<>();
            if (providerArguments != null) {
                int i = 0;
                for (ProviderArgument argument : providerArguments) {
                    Type expectedStateType = genericParameterTypes[i++];
                    states.add(jsonConverter.fromString(argument.getSerializedStateObject(), expectedStateType));
                }
            }
            return states.toArray();
        }

        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static class ProviderArgument {
        private String serializedStateObject;
        private String stateObjectClassName;

        private ProviderArgument(String serializedStateObject, String stateObjectClassName) {
            this.serializedStateObject = serializedStateObject;
            this.stateObjectClassName = stateObjectClassName;
        }

        public String getSerializedStateObject() {
            return serializedStateObject;
        }

        public String getStateObjectClassName() {
            return stateObjectClassName;
        }


        @Override
        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }
}
