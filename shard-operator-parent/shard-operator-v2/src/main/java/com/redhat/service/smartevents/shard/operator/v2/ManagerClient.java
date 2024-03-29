package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface ManagerClient {

    Uni<List<BridgeDTO>> fetchBridgesForDataPlane();

    Uni<HttpResponse<Buffer>> notifyBridgeStatus(List<ResourceStatusDTO> resourceStatusDTOs);

    Uni<List<ProcessorDTO>> fetchProcessorsForDataPlane();

    Uni<HttpResponse<Buffer>> notifyProcessorStatus(List<ResourceStatusDTO> resourceStatusDTOs);
}
