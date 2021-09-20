package com.redhat.service.bridge.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.exceptions.AlreadyExistingItemException;
import com.redhat.service.bridge.manager.exceptions.BridgeLifecycleException;
import com.redhat.service.bridge.manager.exceptions.ItemNotFoundException;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;

@Transactional
@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @Override
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
    public Bridge getBridge(String id) {
        Bridge b = bridgeDAO.findById(id);
        if (b == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' does not exist", id));
        }
        return b;
    }

    private Bridge findByIdAndCustomerId(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", id, customerId));
        }
        return bridge;
    }

    @Override
    public Bridge getBridge(String id, String customerId) {
        return findByIdAndCustomerId(id, customerId);
    }

    @Override
    public void deleteBridge(String id, String customerId) {
        Long processorsCount = processorService.getProcessorsCount(id, customerId);
        if (processorsCount > 0) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }

        Bridge bridge = findByIdAndCustomerId(id, customerId);
        bridge.setStatus(BridgeStatus.DELETION_REQUESTED);
        LOGGER.info("[manager] Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    @Override
    public ListResult<Bridge> getBridges(String customerId, int page, int pageSize) {
        return bridgeDAO.findByCustomerId(customerId, page, pageSize);
    }

    @Override
    public List<Bridge> getBridgesByStatuses(List<BridgeStatus> statuses) {
        return bridgeDAO.findByStatuses(statuses);
    }

    @Override
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
