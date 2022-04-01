package com.redhat.service.bridge.processor.actions.webhook;

import com.redhat.service.bridge.infra.auth.AbstractOidcClient;
import com.redhat.service.bridge.processor.actions.common.ActionInvoker;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookActionInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient client;
    private final AbstractOidcClient oidcClient;

    public WebhookActionInvoker(String endpoint, WebClient client) {
        this(endpoint, client, null);
    }

    public WebhookActionInvoker(String endpoint, WebClient client, AbstractOidcClient oidcClient) {
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
