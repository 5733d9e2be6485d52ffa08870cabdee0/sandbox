package com.redhat.service.smartevents.processor.actions.webhook;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.auth.OidcClient;
import com.redhat.service.smartevents.infra.auth.OidcClientConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class WebhookActionInvokerBuilder implements WebhookAction, ActionInvokerBuilder {

    @Inject
    Instance<OidcClient> oidcClients;

    @Inject
    Vertx vertx;

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String endpoint = action.getParameter(ENDPOINT_PARAM);

        WebClientOptions options = isSslVerificationDisabled(action)
                ? new WebClientOptions().setTrustAll(true).setVerifyHost(false)
                : new WebClientOptions();

        WebClient webClient = getWebClient(options);

        if (requiresTechnicalBearerToken(action)) {
            return new WebhookActionInvoker(endpoint, webClient, getOidcClient());
        }
        if (requiresBasicAuth(action)) {
            String basicAuthUsername = action.getParameter(BASIC_AUTH_USERNAME_PARAM);
            String basicAuthPassword = action.getParameter(BASIC_AUTH_PASSWORD_PARAM);
            return new WebhookActionInvoker(endpoint, webClient, basicAuthUsername, basicAuthPassword);
        }
        return new WebhookActionInvoker(endpoint, webClient);
    }

    private WebClient getWebClient(WebClientOptions options) {
        return WebClient.create(vertx, options.setLogActivity(true));
    }

    private OidcClient getOidcClient() {
        return oidcClients.stream()
                .filter(x -> Objects.equals(x.getName(), OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME))
                .findFirst()
                .orElseThrow(() -> new TechnicalBearerTokenNotConfiguredException("A webhook action needed the webhook oidc client bean but it was not configured."));
    }

    private static boolean isSslVerificationDisabled(Action action) {
        return Boolean.parseBoolean(action.getParameter(SSL_VERIFICATION_DISABLED));
    }

    private static boolean requiresBasicAuth(Action action) {
        return action.hasParameter(BASIC_AUTH_USERNAME_PARAM);
    }

    private static boolean requiresTechnicalBearerToken(Action action) {
        return Boolean.parseBoolean(action.getParameter(USE_TECHNICAL_BEARER_TOKEN_PARAM));
    }
}
