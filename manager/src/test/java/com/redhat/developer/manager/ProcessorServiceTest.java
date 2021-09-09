package com.redhat.developer.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.dao.BridgeDAO;
import com.redhat.developer.manager.dao.ProcessorDAO;
import com.redhat.developer.manager.exceptions.AlreadyExistingItemException;
import com.redhat.developer.manager.exceptions.BridgeLifecycleException;
import com.redhat.developer.manager.exceptions.ItemNotFoundException;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.Processor;

import io.quarkus.test.junit.QuarkusTest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class ProcessorServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ProcessorService processorService;

    private Bridge createBridge(BridgeStatus status) {

        Bridge b = new Bridge();
        b.setName("foo-" + System.currentTimeMillis());
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(status);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    @Test
    public void createProcessor_bridgeNotActive() {
        Bridge b = createBridge(BridgeStatus.PROVISIONING);
        assertThrows(BridgeLifecycleException.class, () -> processorService.createProcessor(b.getId(), b.getCustomerId(), new ProcessorRequest()));
    }

    @Test
    public void createProcessor_bridgeDoesNotExist() {
        assertThrows(ItemNotFoundException.class, () -> processorService.createProcessor("foo", TestConstants.DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
    }

    @Test
    public void createProcessor_processorWithSameNameAlreadyExists() {

        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor");

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor, is(notNullValue()));

        assertThrows(AlreadyExistingItemException.class, () -> processorService.createProcessor(b.getId(), b.getCustomerId(), r));
    }

    @Test
    public void createProcessor() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor");

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor, is(notNullValue()));

        assertThat(processor.getBridge().getId(), equalTo(b.getId()));
        assertThat(processor.getName(), equalTo(r.getName()));
        assertThat(processor.getStatus(), equalTo(BridgeStatus.REQUESTED));
        assertThat(processor.getSubmittedAt(), is(notNullValue()));
    }

    @Test
    @Transactional
    public void getProcessorByStatuses() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor");

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);

        r.setName("My Processor 2");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.AVAILABLE);
        processorDAO.getEntityManager().merge(processor);

        r.setName("My Processor 3");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.DELETION_REQUESTED);
        processorDAO.getEntityManager().merge(processor);

        List<Processor> processors = processorService.getProcessorByStatuses(b.getId(), asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED));
        assertThat(processors, hasSize(2));
        processors.stream().forEach((px) -> assertThat(px.getName(), in(asList("My Processor", "My Processor 3"))));
    }

    @Test
    public void getProcessorByStatuses_bridgeDoesNotExist() {
        assertThrows(ItemNotFoundException.class, () -> processorService.getProcessorByStatuses("foo", asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED)));
    }

    @Test
    public void getProcessorByStatuses_bridgeNotInActiveStatus() {
        Bridge b = createBridge(BridgeStatus.PROVISIONING);
        assertThrows(BridgeLifecycleException.class, () -> processorService.getProcessorByStatuses(b.getId(), asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED)));
    }

    @Test
    public void updateProcessorStatus() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor");

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        ProcessorDTO dto = processor.toDTO();
        dto.setStatus(BridgeStatus.AVAILABLE);

        Processor updated = processorService.updateProcessorStatus(dto);
        assertThat(updated.getStatus(), equalTo(BridgeStatus.AVAILABLE));
    }

    @Test
    public void updateProcessorStatus_bridgeDoesNotExist() {
        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("foo");
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridge(bridge);

        assertThrows(ItemNotFoundException.class, () -> processorService.updateProcessorStatus(processor));
    }

    @Test
    public void updateProcessorStatus_processorDoesNotExist() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridge(b.toDTO());
        processor.setId("foo");

        assertThrows(ItemNotFoundException.class, () -> processorService.updateProcessorStatus(processor));
    }
}
