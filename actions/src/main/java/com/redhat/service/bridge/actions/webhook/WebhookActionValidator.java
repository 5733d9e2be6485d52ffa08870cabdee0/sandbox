package com.redhat.service.bridge.actions.webhook;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class WebhookActionValidator implements ActionParameterValidator {

    public static final String MISSING_ENDPOINT_PARAM_MESSAGE = "Missing or empty \"endpoint\" parameter";
    public static final String INVALID_ENDPOINT_PARAM_MESSAGE = "The supplied \"endpoint\" parameter is not a valid URL";
    public static final String INVALID_PROTOCOL_MESSAGE = "The \"endpoint\" protocol must be either \"http\" or \"https\"";

    @Override
    public ValidationResult isValid(BaseAction baseAction) {
        if (baseAction.getParameters() == null) {
            return ValidationResult.invalid();
        }

        String endpoint = baseAction.getParameters().get(WebhookAction.ENDPOINT_PARAM);
        if (endpoint == null || endpoint.isEmpty()) {
            return ValidationResult.invalid(MISSING_ENDPOINT_PARAM_MESSAGE);
        }

        Optional<URL> optUrl = toURL(endpoint);
        if (!optUrl.isPresent()) {
            return ValidationResult.invalid(INVALID_ENDPOINT_PARAM_MESSAGE);
        }

        String protocol = optUrl.get().getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return ValidationResult.invalid(INVALID_PROTOCOL_MESSAGE);
        }

        return ValidationResult.valid();
    }

    private static Optional<URL> toURL(String input) {
        try {
            return Optional.of(new URL(input));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }
}
