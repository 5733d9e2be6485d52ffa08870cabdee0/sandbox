package com.redhat.developer.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.dao.BridgeDAO;
import com.redhat.developer.manager.dao.ProcessorDAO;
import com.redhat.developer.manager.exceptions.AlreadyExistingItemException;
import com.redhat.developer.manager.exceptions.BridgeLifecycleException;
import com.redhat.developer.manager.exceptions.ItemNotFoundException;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.Processor;

@ApplicationScoped
public class ProcessorService {

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgeDAO bridgeDAO;

    @Transactional
    public Processor createProcessor(String customerId, String bridgeId, ProcessorRequest processorRequest) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(bridgeId, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException("Bridge with id '" + bridgeId + "' does not exist for customer '" + customerId + "'");
        }

        checkBridgeIsInActiveState(bridge);

        Processor p = processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName());
        if (p != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        p = new Processor();
        p.setName(processorRequest.getName());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setStatus(BridgeStatus.REQUESTED);
        bridge.addProcessor(p);
        processorDAO.persist(p);
        return p;
    }

    private void checkBridgeIsInActiveState(Bridge b) {
        if (BridgeStatus.AVAILABLE != b.getStatus()) {
            /* We cannot deploy Processors to a Bridge that is not Available */
            throw new BridgeLifecycleException("Bridge with id '" + b.getId() + "' for customer '" + b.getCustomerId() + "' is not in the '" + BridgeStatus.AVAILABLE + "' state.");
        }
    }

    @Transactional
    public List<Processor> getProcessorByStatuses(String bridgeId, List<BridgeStatus> statuses) {
        Bridge b = bridgeDAO.findById(bridgeId);
        if (b == null) {
            throw new ItemNotFoundException("Bridge with id '" + bridgeId + "' does not exist.");
        }

        checkBridgeIsInActiveState(b);
        return processorDAO.findByStatuses(b.getId(), statuses);
    }
}
