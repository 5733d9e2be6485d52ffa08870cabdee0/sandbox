package com.redhat.service.smartevents.processor.validators.custom;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersNotValidException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.validators.AbstractGatewayValidator;

@ApplicationScoped
public class WebhookActionValidator extends AbstractGatewayValidator implements WebhookAction, CustomGatewayValidator {

    public static final String MALFORMED_ENDPOINT_PARAM_MESSAGE = "Malformed \"endpoint\" URL";
    public static final String BASIC_AUTH_CONFIGURATION_MESSAGE = "Basic authentication configuration error. " +
            "\"" + BASIC_AUTH_USERNAME_PARAM + "\" and \"" + BASIC_AUTH_PASSWORD_PARAM + "\" must be both present and non empty.";
    public static final String INVALID_PROTOCOL_MESSAGE = "The \"endpoint\" protocol must be either \"http\" or \"https\"";

    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";

    @Inject
    public WebhookActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }

    @Override
    public ValidationResult applyAdditionalValidations(Gateway gateway) {
        return additionalValidations(gateway);
    }

    static ValidationResult additionalValidations(Gateway gateway) {
        if (gateway.hasParameter(BASIC_AUTH_USERNAME_PARAM) && !gateway.hasParameter(BASIC_AUTH_PASSWORD_PARAM)
                || !gateway.hasParameter(BASIC_AUTH_USERNAME_PARAM) && gateway.hasParameter(BASIC_AUTH_PASSWORD_PARAM)) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(BASIC_AUTH_CONFIGURATION_MESSAGE));
        }

        if (gateway.hasParameter(BASIC_AUTH_USERNAME_PARAM) && gateway.hasParameter(BASIC_AUTH_PASSWORD_PARAM)
                && (gateway.getParameter(BASIC_AUTH_USERNAME_PARAM).isEmpty() || gateway.getParameter(BASIC_AUTH_PASSWORD_PARAM).isEmpty())) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(BASIC_AUTH_CONFIGURATION_MESSAGE));
        }

        String endpoint = gateway.getParameter(ENDPOINT_PARAM);
        URL endpointUrl;
        try {
            endpointUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(malformedUrlMessage(endpoint, e)));
        }

        String protocol = endpointUrl.getProtocol();
        if (!PROTOCOL_HTTP.equalsIgnoreCase(protocol) && !PROTOCOL_HTTPS.equalsIgnoreCase(protocol)) {
            return ValidationResult.invalid(new ProcessorGatewayParametersNotValidException(invalidProtocolMessage(protocol)));
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
