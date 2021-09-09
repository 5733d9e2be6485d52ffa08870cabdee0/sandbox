package com.redhat.developer.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.developer.manager.dao.ProcessorDAO;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.dao.BridgeDAO;
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
        b.setCustomerId("bar");
        b.setStatus(status);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    @Test
    public void createProcessor_bridgeNotActive() {
        Bridge b = createBridge(BridgeStatus.PROVISIONING);
        assertThrows(BridgeLifecycleException.class, () -> processorService.createProcessor(b.getCustomerId(), b.getId(), new ProcessorRequest()));
    }

    @Test
    public void createProcessor_bridgeDoesNotExist() {
        assertThrows(ItemNotFoundException.class, () -> processorService.createProcessor("foo", "bar", new ProcessorRequest()));
    }

    @Test
    public void createProcessor_processorWithSameNameAlreadyExists() {

        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest();
        r.setName("My Processor");

        Processor processor = processorService.createProcessor(b.getCustomerId(), b.getId(), r);
        assertThat(processor, is(notNullValue()));

        assertThrows(AlreadyExistingItemException.class, () -> processorService.createProcessor(b.getCustomerId(), b.getId(), r));
    }

    @Test
    public void createProcessor() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor");

        Processor processor = processorService.createProcessor(b.getCustomerId(), b.getId(), r);
        assertThat(processor, is(notNullValue()));

        assertThat(processor.getBridge().getId(), equalTo(b.getId()));
        assertThat(processor.getName(), equalTo(r.getName()));
        assertThat(processor.getStatus(), equalTo(BridgeStatus.REQUESTED));
        assertThat(processor.getSubmittedAt(), is(notNullValue()));
    }

    @Test
    @Transactional
    public void findProcessorByStatuses() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);

        ProcessorRequest r = new ProcessorRequest("number 1");
        processorService.createProcessor(b.getCustomerId(), b.getId(), r);

        r.setName("number 2");
        Processor p = processorService.createProcessor(b.getCustomerId(), b.getId(), r);
        p.setStatus(BridgeStatus.DELETION_REQUESTED);
        processorDAO.getEntityManager().merge(p);

        r.setName("number 3");
        p = processorService.createProcessor(b.getCustomerId(), b.getId(), r);
        p.setStatus(BridgeStatus.AVAILABLE);
        processorDAO.getEntityManager().merge(p);

        List<Processor> processors = processorService.getProcessorByStatuses(b.getId(), asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED));
        assertThat(processors, hasSize(2));
        processors.stream().forEach((px) -> assertThat(px.getName(), in(asList("number 1", "number 2"))));
    }

    @Test
    public void findProcessorByStatuses_bridgeIsNotActive() {
        Bridge b = createBridge(BridgeStatus.PROVISIONING);
        assertThrows(BridgeLifecycleException.class, () -> processorService.getProcessorByStatuses(b.getId(), asList(BridgeStatus.PROVISIONING, BridgeStatus.DELETION_REQUESTED)));
    }

    @Test
    public void findProcessorsByStatuses_bridgeDoesNotExist() {
        assertThrows(ItemNotFoundException.class, () -> processorService.getProcessorByStatuses("foo", asList(BridgeStatus.PROVISIONING, BridgeStatus.DELETION_REQUESTED)));
    }
}
