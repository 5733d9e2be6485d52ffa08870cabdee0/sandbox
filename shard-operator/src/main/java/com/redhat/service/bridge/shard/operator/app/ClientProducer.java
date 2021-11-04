package com.redhat.service.bridge.shard.operator.app;

import io.fabric8.openshift.client.OpenShiftClient;

public interface ClientProducer {
    OpenShiftClient produceClient();
}