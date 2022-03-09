package com.redhat.service.bridge.manager;

import java.util.List;
import java.util.Optional;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.models.Bridge;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    /**
     * Provides Bridge instance for given id/name. It throws `ItemNotFoundException` if bridge instance not found.
     * 
     * @param bridgeIdentifier It could be a bridge Id or bridge name.
     * @param customerId Customer id.
     * @return Bridge instance.
     */
    Bridge getBridgeByBridgeIdentifier(String bridgeIdentifier, String customerId);

    /**
     * Provide Bridge instance for given bridge id.
     * 
     * @param id Bridge id.
     * @param customerId Customer id.
     * @return Optional Bridge instance.
     */
    Optional<Bridge> getBridgeByIdAndCustomerId(String id, String customerId);

    /**
     * Return `True` if Bridge is in ready state else return `false`.
     * 
     * @param bridge Bridge instance
     * @return `True` if Bridge is in ready state else return `false`
     */
    boolean isBridgeReady(Bridge bridge);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo);

    List<Bridge> getBridgesByStatusesAndShardId(List<BridgeStatus> statuses, String shardId);

    Bridge updateBridge(BridgeDTO bridgeDTO);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);

    String getBridgeTopicName(Bridge bridge);
}
