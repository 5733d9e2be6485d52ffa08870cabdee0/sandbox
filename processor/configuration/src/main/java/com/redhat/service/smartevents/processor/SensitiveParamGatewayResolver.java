package com.redhat.service.smartevents.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

/**
 * This resolver acts a base implementation for Sources or Actions that contain sensitive parameters. It ensures
 * that any sensitive parameters are masked on the original user request and then made available to be stored into
 * a Vault implementation.
 *
 * @param <T> - The type of Gateway supported by this Resolver.
 */
public abstract class SensitiveParamGatewayResolver<T extends Gateway> implements GatewayResolver<T> {

    /**
     * The default mask we apply to values of sensitive parameters submitted by the user.
     */
    static final String DEFAULT_SENSITIVE_PARAMETER_VALUE_MASK = "*****";

    protected abstract Action resolveActionWithoutSensitiveParameters(T gateway, String customerId, String bridgeId, String processorId);

    protected abstract Set<String> getSensitiveParameterNames();

    @Override
    public ResolvedGateway<T> resolve(T gateway, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = resolveActionWithoutSensitiveParameters(gateway, customerId, bridgeId, processorId);
        Set<String> sensitiveParameterNames = getSensitiveParameterNames();
        if (sensitiveParameterNames.isEmpty()) {
            return new ResolvedGateway<>(gateway, resolvedAction);
        }

        Map<String, String> sensitiveParameters = new HashMap<>();
        for (String param : sensitiveParameterNames) {
            String value = gateway.getParameters().get(param);
            if (value != null) {

                // Store the value for the sensitive param we need to push to the Vault
                sensitiveParameters.put(param, value);

                /*
                 * Apply a default mask to the sensitive value submitted by the user so we
                 * 1. Don't store sensitive values in plaintext in the DB
                 * 2. Don't return sensitive values in plaintext from the API
                 */
                gateway.getParameters().put(param, DEFAULT_SENSITIVE_PARAMETER_VALUE_MASK);

                // Ensure that we are storing sensitive values on the resolved Action configuration
                resolvedAction.getParameters().remove(param);
            }
        }

        return new ResolvedGateway<>(gateway, resolvedAction, sensitiveParameters);
    }
}
