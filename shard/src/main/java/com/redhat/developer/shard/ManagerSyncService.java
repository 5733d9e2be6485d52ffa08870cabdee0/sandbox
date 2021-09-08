package com.redhat.developer.shard;

import com.redhat.developer.infra.dto.BridgeDTO;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface ManagerSyncService {
    Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO);

    Uni<Object> fetchAndProcessBridgesFromManager();


}
