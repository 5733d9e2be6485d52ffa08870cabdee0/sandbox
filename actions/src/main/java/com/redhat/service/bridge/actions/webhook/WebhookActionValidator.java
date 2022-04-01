package com.redhat.service.bridge.actions.webhook;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

@ApplicationScoped
public class WebhookActionValidator implements ActionParameterValidator {

    public static final String MISSING_ENDPOINT_PARAM_MESSAGE = "Missing or empty \"endpoint\" parameter";
    public static final String MALFORMED_ENDPOINT_PARAM_MESSAGE = "Malformed \"endpoint\" URL";
    public static final String INVALID_PROTOCOL_MESSAGE = "The \"endpoint\" protocol must be either \"http\" or \"https\"";
    public static final String RESERVED_ATTRIBUTES_USAGE_MESSAGE = "Some reserved parameters have been added to the request.";

    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";

    @Override
    public String getType() {
        return WebhookAction.TYPE;
    }

    @Override
    public ValidationResult isValid(BaseAction baseAction) {
        if (baseAction.getParameters() == null) {
            return ValidationResult.invalid();
        }

        String endpoint = baseAction.getParameters().get(WebhookAction.ENDPOINT_PARAM);
        if (endpoint == null || endpoint.isEmpty()) {
            return ValidationResult.invalid(MISSING_ENDPOINT_PARAM_MESSAGE);
        }

        if (baseAction.getParameters().containsKey(WebhookAction.USE_TECHNICAL_BEARER_TOKEN)) {
            return ValidationResult.invalid(RESERVED_ATTRIBUTES_USAGE_MESSAGE);
        }

        URL endpointUrl;
        try {
            endpointUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            return ValidationResult.invalid(malformedUrlMessage(endpoint, e));
        }

        String protocol = endpointUrl.getProtocol();
        if (!PROTOCOL_HTTP.equalsIgnoreCase(protocol) && !PROTOCOL_HTTPS.equalsIgnoreCase(protocol)) {
            return ValidationResult.invalid(invalidProtocolMessage(protocol));
        }

        return ValidationResult.valid();
    }

    private static String invalidProtocolMessage(String actualProtocol) {
        return String.format("%s (found: \"%s\")", INVALID_PROTOCOL_MESSAGE, actualProtocol);
    }

    private static String malformedUrlMessage(String endpoint, MalformedURLException exception) {
        return String.format("%s \"%s\" (%s)", MALFORMED_ENDPOINT_PARAM_MESSAGE, endpoint, exception.getMessage());
    }
}
