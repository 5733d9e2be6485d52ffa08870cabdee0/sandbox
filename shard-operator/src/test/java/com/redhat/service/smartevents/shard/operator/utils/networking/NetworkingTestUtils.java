package com.redhat.service.smartevents.shard.operator.utils.networking;

import io.fabric8.kubernetes.api.model.Namespaced;

public interface NetworkingTestUtils {
    Namespaced getNetworkResource(String name, String namespace);

    void patchNetworkResource(String name, String namespace);

    void cleanUp();
}
