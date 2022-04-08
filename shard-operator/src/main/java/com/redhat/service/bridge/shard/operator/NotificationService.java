package com.redhat.service.bridge.shard.operator;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestType;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

public interface NotificationService {
    Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO);

    Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO);

    void updateManagerRequestMetricsOnSuccess(ManagerRequestType requestType, HttpResponse<Buffer> successResponse);

    void updateManagerRequestMetricsOnFailure(ManagerRequestType requestType, Throwable error);

}
