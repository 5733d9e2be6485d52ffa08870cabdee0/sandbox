package com.redhat.service.smartevents.processor.actions.webhook;

import javax.ws.rs.core.Response;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.auth.OidcClient;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.errorhandler.ErrorMetadata;
import com.redhat.service.smartevents.processor.errorhandler.ErrorPublisher;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

import static com.redhat.service.smartevents.processor.errorhandler.ErrorMetadata.ErrorType.ERROR;

public class WebhookActionInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient webClient;
    private final OidcClient oidcClient;
    private final ErrorPublisher errorPublisher;
    private final String basicAuthUsername;
    private final String basicAuthPassword;

    public WebhookActionInvoker(String endpoint, WebClient webClient, ErrorPublisher errorPublisher) {
        this(endpoint, webClient, errorPublisher, null, null, null);
    }

    public WebhookActionInvoker(String endpoint, WebClient webClient, ErrorPublisher errorPublisher, OidcClient oidcClient) {
        this(endpoint, webClient, errorPublisher, oidcClient, null, null);
    }

    public WebhookActionInvoker(String endpoint, WebClient webClient, ErrorPublisher errorPublisher, String basicAuthUsername, String basicAuthPassword) {
        this(endpoint, webClient, errorPublisher, null, basicAuthUsername, basicAuthPassword);
    }

    private WebhookActionInvoker(String endpoint, WebClient webClient, ErrorPublisher errorPublisher, OidcClient oidcClient, String basicAuthUsername, String basicAuthPassword) {
        this.endpoint = endpoint;
        this.webClient = webClient;
        this.oidcClient = oidcClient;
        this.errorPublisher = errorPublisher;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    @Override
    public void onEvent(String bridgeId, String processorId, String originalEventId, String transformedEvent) {
        HttpRequest<Buffer> request = webClient.postAbs(endpoint);
        if (oidcClient != null) {
            String token = oidcClient.getToken();
            if (token != null && !"".equals(token)) {
                request = request.bearerTokenAuthentication(token);
            }
        } else if (basicAuthUsername != null) {
            request.basicAuthentication(basicAuthUsername, basicAuthPassword);
        }

        // Add our HTTP Headers.
        // This can be replaced with w3c trace-context parameters when we add distributed tracing.
        request.headers().add(APIConstants.X_RHOSE_BRIDGE_ID, bridgeId);
        request.headers().add(APIConstants.X_RHOSE_PROCESSOR_ID, processorId);
        request.headers().add(APIConstants.X_RHOSE_ORIGINAL_EVENT_ID, originalEventId);

        request.sendJson(new JsonObject(transformedEvent))
                .subscribe()
                .with(response -> {
                    final int statusCode = response.statusCode();
                    if (Response.Status.fromStatusCode(statusCode).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        errorPublisher.sendError(new ErrorMetadata(bridgeId, processorId, originalEventId, ERROR),
                                transformedEvent,
                                new HTTPResponseException(response.statusMessage(), statusCode));
                    }
                });
    }

    String getEndpoint() {
        return endpoint;
    }

    WebClient getWebClient() {
        return webClient;
    }

    OidcClient getOidcClient() {
        return oidcClient;
    }

    String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    String getBasicAuthPassword() {
        return basicAuthPassword;
    }
}
