package com.redhat.service.smartevents.manager;

import java.util.List;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.QuotaType;

public interface BridgesService {

    Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    Bridge getBridge(String id, String customerId);

    Bridge getReadyBridge(String bridgeId, String customerId);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo);

    List<Bridge> findByShardIdWithReadyDependencies(String shardId);

    Bridge updateBridge(BridgeDTO bridgeDTO);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);

    /**
     * Provide count of bridges whose expireAt date is in future or null.
     * 
     * @param orgId Organisation Id
     * @param instanceType instance type.
     * @return Active bridge count.
     */
    Long getActiveBridgeCount(String orgId, QuotaType instanceType);

    /**
     * Check whether bridge for given id is expire or not.
     * 
     * @param id Bridge Id.
     * @return @True if active else @false.
     */
    boolean isBridgeActive(String id);
}
