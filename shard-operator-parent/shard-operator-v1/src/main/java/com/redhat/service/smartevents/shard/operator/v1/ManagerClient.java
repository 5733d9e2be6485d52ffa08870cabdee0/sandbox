package com.redhat.service.smartevents.shard.operator.v1;

import java.util.List;

import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface ManagerClient {

    Uni<List<BridgeDTO>> fetchBridgesToDeployOrDelete();

    Uni<List<ProcessorDTO>> fetchProcessorsToDeployOrDelete();

    Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(ManagedResourceStatusUpdateDTO dto);

    Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorManagedResourceStatusUpdateDTO dto);
}
