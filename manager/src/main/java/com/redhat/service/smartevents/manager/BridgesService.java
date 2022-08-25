package com.redhat.service.smartevents.manager;

import java.util.List;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessingErrorResponse;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ProcessingError;

public interface BridgesService {

    String ENDPOINT_ERROR_HANDLER_TYPE = "endpoint";

    Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest);

    Bridge updateBridge(String bridgeId, String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    Bridge getBridge(String id, String customerId);

    Bridge getReadyBridge(String bridgeId, String customerId);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo);

    List<Bridge> findByShardIdWithReadyDependencies(String shardId);

    Bridge updateBridge(ManagedResourceStatusUpdateDTO updateDTO);

    ListResult<ProcessingError> getBridgeErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);

    ProcessingErrorResponse toResponse(ProcessingError processingError);
}
