package com.redhat.service.smartevents.shard.operator.v2.resources.camel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("camel.apache.org")
@Kind("Integration")
public class CamelIntegration extends CustomResource<CamelIntegrationSpec, CamelIntegrationStatus> implements Namespaced {
    @JsonIgnore
    public boolean isReady() {
        return status != null && status.isReady();
    }
}
