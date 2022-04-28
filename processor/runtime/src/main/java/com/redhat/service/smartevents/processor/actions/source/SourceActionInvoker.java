package com.redhat.service.smartevents.processor.actions.source;

import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.auth.OidcClient;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventSerializationException;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class SourceActionInvoker implements ActionInvoker {

    public static final String CLOUD_EVENT_SOURCE = "RHOAS";

    private static final Logger LOG = LoggerFactory.getLogger(SourceActionInvoker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Supplier<String> UUID_GENERATOR = () -> UUID.randomUUID().toString();

    private final String endpoint;
    private final String cloudEventType;
    private final WebClient client;
    private final OidcClient oidcClient;
    private final Supplier<String> idGenerator;

    public SourceActionInvoker(String endpoint, String cloudEventType, WebClient client, OidcClient oidcClient) {
        this(endpoint, cloudEventType, client, oidcClient, UUID_GENERATOR);
    }

    SourceActionInvoker(String endpoint, String cloudEventType, WebClient client, OidcClient oidcClient, Supplier<String> idGenerator) {
        this.endpoint = endpoint;
        this.cloudEventType = cloudEventType;
        this.client = client;
        this.oidcClient = oidcClient;
        this.idGenerator = idGenerator;
    }

    @Override
    public void onEvent(String event) {
        HttpRequest<Buffer> request = client.postAbs(endpoint);
        String token = oidcClient.getToken();
        if (token == null || token.isEmpty()) {
            throw new TechnicalBearerTokenNotConfiguredException("The configured OIDC client returned an empty token.");
        }
        request = request.bearerTokenAuthentication(token);
        request.sendJsonObject(getPayload(event)).subscribe().with(
                response -> LOG.debug("Successfully sent event (response: {} {})", response.statusCode(), response.statusMessage()),
                error -> LOG.error("Error when sending event", error));
    }

    private JsonObject getPayload(String event) {
        try {
            ObjectNode cloudEvent = MAPPER.createObjectNode();
            cloudEvent.set("id", new TextNode(idGenerator.get()));
            // TODO: set a source that reflects the actual processor pod
            cloudEvent.set("source", new TextNode(CLOUD_EVENT_SOURCE));
            cloudEvent.set("specversion", new TextNode("1.0"));
            cloudEvent.set("type", new TextNode(cloudEventType));
            cloudEvent.set("data", MAPPER.readTree(event));

            String payloadString = encode(cloudEvent);
            return new JsonObject(payloadString);
        } catch (JsonProcessingException e) {
            throw new CloudEventDeserializationException("Failed to wrap cloud event");
        }
    }

    private static String encode(ObjectNode event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new CloudEventSerializationException("Failed to encode CloudEvent");
        }
    }
}
