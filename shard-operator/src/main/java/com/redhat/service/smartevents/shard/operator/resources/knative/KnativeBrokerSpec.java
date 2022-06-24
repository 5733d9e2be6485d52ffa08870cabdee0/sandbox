package com.redhat.service.smartevents.shard.operator.resources.knative;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeBrokerSpec {

    private KnativeBrokerSpecConfig config;

    public KnativeBrokerSpecConfig getConfig() {
        return config;
    }

    public void setConfig(KnativeBrokerSpecConfig config) {
        this.config = config;
    }
}
