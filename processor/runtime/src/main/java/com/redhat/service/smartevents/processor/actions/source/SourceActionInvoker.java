package com.redhat.service.smartevents.processor.actions.source;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventSerializationException;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class SourceActionInvoker implements ActionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(SourceActionInvoker.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final String cloudEventType;
    private final WebClient client;
    private final AbstractOidcClient oidcClient;

    public SourceActionInvoker(String endpoint, String cloudEventType, WebClient client, AbstractOidcClient oidcClient) {
        this.endpoint = endpoint;
        this.cloudEventType = cloudEventType;
        this.client = client;
        this.oidcClient = oidcClient;
    }

    @Override
    public void onEvent(String event) {
        LOG.info("SourceActionInvoker::onEvent | {} - {} | {}", endpoint, cloudEventType, event);

        HttpRequest<Buffer> request = client.postAbs(endpoint);
        String token = oidcClient.getToken();
        LOG.info("SourceActionInvoker::onEvent | Token: {}", token);
        if (token != null && !"".equals(token)) {
            LOG.info("SourceActionInvoker::onEvent | Token configured");
            request = request.bearerTokenAuthentication(token);
        }
        request.sendJsonObject(getPayload(event)).subscribe().with(
                response -> LOG.info("RESPONSE {} {}", response.statusCode(), response.statusMessage()),
                error -> LOG.error("ERROR", error));
    }

    private JsonObject getPayload(String event) {
        try {
            ObjectNode cloudEvent = MAPPER.createObjectNode();
            cloudEvent.set("id", new TextNode(UUID.randomUUID().toString()));
            cloudEvent.set("source", new TextNode("RHOAS"));
            cloudEvent.set("specversion", new TextNode("1.0"));
            cloudEvent.set("type", new TextNode(cloudEventType));
            cloudEvent.set("data", MAPPER.readTree(event));

            String payloadString = encode(cloudEvent);
            LOG.info("PAYLOAD: {}", payloadString);
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
