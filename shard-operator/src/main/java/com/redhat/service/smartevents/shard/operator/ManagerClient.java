package com.redhat.service.smartevents.shard.operator;

import java.util.List;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface ManagerClient {

    Uni<List<BridgeDTO>> fetchBridgesToDeployOrDelete();

    Uni<List<ProcessorDTO>> fetchProcessorsToDeployOrDelete();

    Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO);

    Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO);
}
