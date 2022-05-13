package com.redhat.service.smartevents.processor;

import java.util.HashMap;
import java.util.Map;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public class ResolvedGateway<T extends Gateway> {

    private T sanitizedUserRequest;

    private Action sanitizedResolvedAction;

    private Map<String, String> sensitiveParameters;

    public ResolvedGateway(T sanitizedUserRequest, Action sanitizedResolvedAction) {
        this(sanitizedUserRequest, sanitizedResolvedAction, new HashMap<>());
    }

    public ResolvedGateway(T sanitizedUserRequest, Action sanitizedResolvedAction, Map<String, String> sensitiveParameters) {
        this.sanitizedUserRequest = sanitizedUserRequest;
        this.sanitizedResolvedAction = sanitizedResolvedAction;
        this.sensitiveParameters = sensitiveParameters;
    }

    /*
        Returns the original requested Gateway from the user with all sensitive parameters sanitized
     */
    public T getSanitizedRequest() {
        return sanitizedUserRequest;
    }

    /*
        Returns the resolved Action for the Gateway, with all parameters except those that are sensitive
     */
    public Action getSanitizedResolvedAction() {
        return sanitizedResolvedAction;
    }

    public Map<String, String> getSensitiveParameters() {
        return sensitiveParameters;
    }
}
