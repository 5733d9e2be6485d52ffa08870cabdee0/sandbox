package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.exceptions.AlreadyExistingItemException;
import com.redhat.service.bridge.manager.exceptions.BridgeLifecycleException;
import com.redhat.service.bridge.manager.exceptions.ItemNotFoundException;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@Transactional
@ApplicationScoped
public class ProcessorServiceImpl implements ProcessorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorServiceImpl.class);

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgesService bridgesService;

    @Inject
    MeterRegistry meterRegistry;

    @Override
    public Processor getProcessor(String processorId, String bridgeId, String customerId) {

        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(processorId, bridge.getId(), bridge.getCustomerId());
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on Bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }

        return processor;
    }

    @Override
    public Processor createProcessor(String bridgeId, String customerId, ProcessorRequest processorRequest) {
        Bridge bridge = getAvailableBridge(bridgeId, customerId);
        if (processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        ProcessorDefinition definition = new ProcessorDefinition(processorRequest.getFilters(), processorRequest.getTransformationTemplate(), processorRequest.getAction());

        Processor p = new Processor();

        p.setName(processorRequest.getName());
        p.setDefinition(definition);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setStatus(BridgeStatus.REQUESTED);
        p.setBridge(bridge);

        processorDAO.persist(p);
        return p;
    }

    @Override
    public List<Processor> getProcessorByStatuses(List<BridgeStatus> statuses) {
        return processorDAO.findByStatuses(statuses);
    }

    @Override
    public Processor updateProcessorStatus(ProcessorDTO processorDTO) {
        Bridge bridge = bridgesService.getBridge(processorDTO.getBridge().getId());
        Processor p = processorDAO.findById(processorDTO.getId());
        if (p == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist for Bridge '%s' for customer '%s'", bridge.getId(), bridge.getCustomerId(),
                    processorDTO.getBridge().getCustomerId()));
        }
        p.setStatus(processorDTO.getStatus());

        // Update metrics
        meterRegistry.counter("manager.processor.status.change",
                Collections.singletonList(Tag.of("status", processorDTO.getStatus().toString()))).increment();

        if (processorDTO.getStatus().equals(BridgeStatus.DELETED)) {
            processorDAO.deleteById(processorDTO.getId());
        }

        return p;
    }

    @Override
    public Long getProcessorsCount(String bridgeId, String customerId) {
        return processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
    }

    @Override
    public ListResult<Processor> getProcessors(String bridgeId, String customerId, int page, int size) {
        Bridge bridge = getAvailableBridge(bridgeId, customerId);
        return processorDAO.findByBridgeIdAndCustomerId(bridge.getId(), bridge.getCustomerId(), page, size);
    }

    @Override
    public void deleteProcessor(String bridgeId, String processorId, String customerId) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(processorId, bridgeId, customerId);
        processor.setStatus(BridgeStatus.DELETION_REQUESTED);
        LOGGER.info("[manager] Processor with id '{}' for customer '{}' on bridge '{}' has been marked for deletion", processor.getId(), processor.getBridge().getCustomerId(),
                processor.getBridge().getId());
    }

    private Bridge getAvailableBridge(String bridgeId, String customerId) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        if (BridgeStatus.AVAILABLE != bridge.getStatus()) {
            /* We cannot deploy Processors to a Bridge that is not Available */
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), BridgeStatus.AVAILABLE));
        }

        return bridge;
    }
}
