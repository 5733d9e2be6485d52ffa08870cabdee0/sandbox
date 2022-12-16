package com.redhat.service.smartevents.shard.operator.v2.utils.networking;

import io.fabric8.kubernetes.api.model.Namespaced;

public interface V2NetworkingTestUtils {
    Namespaced getNetworkResource(String name, String namespace);

    void patchNetworkResource(String name, String namespace);

    void cleanUp();
}
