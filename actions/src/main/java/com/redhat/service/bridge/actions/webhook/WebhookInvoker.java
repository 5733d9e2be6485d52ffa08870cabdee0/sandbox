package com.redhat.service.bridge.actions.webhook;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.infra.auth.AbstractOidcClient;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient client;
    private final AbstractOidcClient oidcClient;

    public WebhookInvoker(String endpoint, WebClient client) {
        this(endpoint, client, null);
    }

    public WebhookInvoker(String endpoint, WebClient client, AbstractOidcClient oidcClient) {
        this.endpoint = endpoint;
        this.client = client;
        this.oidcClient = oidcClient;
    }

    @Override
    public void onEvent(String event) {
        HttpRequest<Buffer> request = client.postAbs(endpoint);
        if (oidcClient != null) {
            String token = oidcClient.getToken();
            if (token != null && !"".equals(token)) {
                request = request.bearerTokenAuthentication(token);
            }
        }
        request.sendJsonObjectAndForget(new JsonObject(event));
    }
}
