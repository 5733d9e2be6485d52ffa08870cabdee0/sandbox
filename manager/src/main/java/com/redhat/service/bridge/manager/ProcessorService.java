package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.exceptions.AlreadyExistingItemException;
import com.redhat.service.bridge.manager.exceptions.BridgeLifecycleException;
import com.redhat.service.bridge.manager.exceptions.ItemNotFoundException;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

@Transactional
@ApplicationScoped
public class ProcessorService {

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgesService bridgesService;

    public Processor createProcessor(String bridgeId, String customerId, ProcessorRequest processorRequest) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        checkBridgeInActiveStatus(bridge);

        Processor p = processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName());
        if (p != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        p = new Processor();
        p.setName(processorRequest.getName());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setStatus(BridgeStatus.REQUESTED);
        p.setBridge(bridge);
        processorDAO.persist(p);
        return p;
    }

    public List<Processor> getProcessorByStatuses(String bridgeId, List<BridgeStatus> statuses) {
        Bridge bridge = bridgesService.getBridge(bridgeId);
        checkBridgeInActiveStatus(bridge);
        return processorDAO.findByStatuses(bridge.getId(), statuses);
    }

    private void checkBridgeInActiveStatus(Bridge bridge) {
        if (BridgeStatus.AVAILABLE != bridge.getStatus()) {
            /* We cannot deploy Processors to a Bridge that is not Available */
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), BridgeStatus.AVAILABLE));
        }
    }

    public Processor updateProcessorStatus(ProcessorDTO processorDTO) {
        Bridge bridge = bridgesService.getBridge(processorDTO.getBridge().getId());
        Processor p = processorDAO.findById(processorDTO.getId());
        if (p == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist for Bridge '%s' for customer '%s'", processorDTO.getId(), bridge.getId(), bridge.getCustomerId()));
        }
        p.setStatus(processorDTO.getStatus());
        return p;
    }
}
