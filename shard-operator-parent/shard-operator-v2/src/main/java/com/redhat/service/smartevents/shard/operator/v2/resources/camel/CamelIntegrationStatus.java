package com.redhat.service.smartevents.shard.operator.v2.resources.camel;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.v2.resources.BaseResourceStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamelIntegrationStatus extends BaseResourceStatus {

    private final static String INTEGRATION_KIT_AVAILABLE = "IntegrationKitAvailable";
    private final static String INTEGRATION_PLATFORM_AVAILABLE = "IntegrationPlatformAvailable";
    private final static String KNATIVE_SERVICE_AVAILABLE = "KnativeServiceAvailable";
    private final static String KNATIVE_SERVICE_READY = "KnativeServiceReady";

    private static Set<Condition> getCreationConditions() {
        Set<Condition> conditions = new HashSet<>();
        conditions.add(new Condition(INTEGRATION_KIT_AVAILABLE, ConditionStatus.Unknown));
        conditions.add(new Condition(INTEGRATION_PLATFORM_AVAILABLE, ConditionStatus.Unknown));
        conditions.add(new Condition(KNATIVE_SERVICE_AVAILABLE, ConditionStatus.Unknown));
        conditions.add(new Condition(KNATIVE_SERVICE_READY, ConditionStatus.Unknown));
        return conditions;
    }

    public CamelIntegrationStatus(Set<Condition> initialConditions) {
        super(getCreationConditions());
    }

}
