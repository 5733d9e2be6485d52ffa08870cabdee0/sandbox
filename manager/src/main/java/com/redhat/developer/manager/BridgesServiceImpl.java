package com.redhat.developer.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.dao.BridgeDAO;
import com.redhat.developer.manager.exceptions.AlreadyExistingItemException;
import com.redhat.developer.manager.exceptions.ItemNotFoundException;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.ListResult;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Override
    @Transactional
    public Bridge createBridge(String customerId, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setStatus(BridgeStatus.REQUESTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setCustomerId(customerId);
        bridgeDAO.persist(bridge);
        LOGGER.info("[manager] Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());
        return bridge;
    }

    @Override
    public Bridge getBridge(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge '%s' for customer '%s' does not exist", id, customerId));
        }

        return bridge;
    }

    @Override
    @Transactional
    public void deleteBridge(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge '%s' for customer '%s' does not exist", id, customerId));
        }

        bridge.setStatus(BridgeStatus.DELETION_REQUESTED);
        bridgeDAO.getEntityManager().merge(bridge);
        LOGGER.info("[manager] Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    @Override
    public ListResult<Bridge> getBridges(String customerId, int page, int pageSize) {
        return bridgeDAO.listByCustomerId(customerId, page, pageSize);
    }

    @Override
    public List<Bridge> getBridgesByStatus(BridgeStatus status) {
        return bridgeDAO.findByStatus(status);
    }

    @Override
    @Transactional
    public Bridge updateBridge(Bridge bridge) {
        if (bridge.getStatus().equals(BridgeStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
            return bridge;
        }

        bridgeDAO.getEntityManager().merge(bridge);
        LOGGER.info("[manager] Bridge with id '{}' has been updated for customer '{}'", bridge.getId(), bridge.getCustomerId());
        return bridge;
    }
}
