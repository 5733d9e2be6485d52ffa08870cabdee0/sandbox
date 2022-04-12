package com.redhat.service.rhose.shard.operator;

import com.redhat.service.rhose.infra.models.dto.BridgeDTO;
import com.redhat.service.rhose.infra.models.dto.ProcessorDTO;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface ManagerSyncService {
    Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO);

    Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO);

    Uni<Object> fetchAndProcessBridgesToDeployOrDelete();

    Uni<Object> fetchAndProcessProcessorsToDeployOrDelete();
}
