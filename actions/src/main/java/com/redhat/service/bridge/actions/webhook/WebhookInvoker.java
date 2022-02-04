package com.redhat.service.bridge.actions.webhook;

import com.redhat.service.bridge.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient client;
    private final String token;

    public WebhookInvoker(String endpoint, WebClient client) {
        this(endpoint, client, null);
    }

    public WebhookInvoker(String endpoint, WebClient client, String token) {
        this.endpoint = endpoint;
        this.client = client;
        this.token = token;
    }

    @Override
    public void onEvent(String event) {
        HttpRequest<Buffer> request = client.postAbs(endpoint);
        if (token != null && !"".equals(token)) {
            request = request.bearerTokenAuthentication(token);
        }
        request.sendJsonObjectAndForget(new JsonObject(event));
    }
}
