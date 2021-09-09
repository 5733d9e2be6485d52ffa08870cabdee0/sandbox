package com.redhat.developer.manager;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.dao.BridgeDAO;
import com.redhat.developer.manager.exceptions.AlreadyExistingItemException;
import com.redhat.developer.manager.exceptions.BridgeLifecycleException;
import com.redhat.developer.manager.exceptions.ItemNotFoundException;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.Processor;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class ProcessorServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    private Bridge createBridge(BridgeStatus status) {

        Bridge b = new Bridge();
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
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
        assertThrows(ItemNotFoundException.class, () -> processorService.createProcessor(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
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
}
