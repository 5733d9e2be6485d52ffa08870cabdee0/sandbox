package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.redhat.service.bridge.manager.models.Filter;
import com.redhat.service.bridge.manager.models.Processor;

@Transactional
@ApplicationScoped
public class ProcessorService {

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgesService bridgesService;

    public Processor getProcessor(String processorId, String bridgeId, String customerId) {

        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(processorId, bridge.getId(), bridge.getCustomerId());
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on Bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }

        return processor;
    }

    public Processor createProcessor(String bridgeId, String customerId, ProcessorRequest processorRequest) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        checkBridgeInActiveStatus(bridge);

        if (processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        final Processor p = new Processor();
        Set<Filter> filters = processorRequest.getFilters().stream().map(x -> new Filter(x.getKey(), x.getType(), x.getStringValue(), p)).collect(Collectors.toSet());
        p.setName(processorRequest.getName());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setStatus(BridgeStatus.REQUESTED);
        p.setBridge(bridge);
        p.setFilters(filters);
        processorDAO.persist(p);
        return p;
    }

    public List<Processor> getProcessorByStatuses(List<BridgeStatus> statuses) {
        return processorDAO.findByStatuses(statuses);
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
