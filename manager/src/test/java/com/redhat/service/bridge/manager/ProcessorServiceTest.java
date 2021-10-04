package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.exceptions.AlreadyExistingItemException;
import com.redhat.service.bridge.manager.exceptions.BridgeLifecycleException;
import com.redhat.service.bridge.manager.exceptions.ItemNotFoundException;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class ProcessorServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    void cleanUp() {
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

    private BaseAction createKafkaAction() {
        BaseAction a = new BaseAction();
        a.setName(TestConstants.DEFAULT_ACTION_NAME);
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);
        return a;
    }

    @Test
    void createProcessor_bridgeNotActive() {
        Bridge b = createBridge(BridgeStatus.PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> processorService.createProcessor(b.getId(), b.getCustomerId(), new ProcessorRequest()));
    }

    @Test
    void createProcessor_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CUSTOMER_ID, new ProcessorRequest()));
    }

    @Test
    void createProcessor_processorWithSameNameAlreadyExists() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(AlreadyExistingItemException.class).isThrownBy(() -> processorService.createProcessor(b.getId(), b.getCustomerId(), r));
    }

    @Test
    void createProcessor() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", null, "{}", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThat(processor.getBridge().getId()).isEqualTo(b.getId());
        assertThat(processor.getName()).isEqualTo(r.getName());
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getTransformationTemplate()).isEqualTo("{}");
    }

    @Test
    @Transactional
    void getProcessorByStatuses() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);

        r.setName("My Processor 2");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.AVAILABLE);
        processorDAO.getEntityManager().merge(processor);

        r.setName("My Processor 3");
        processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        processor.setStatus(BridgeStatus.DELETION_REQUESTED);
        processorDAO.getEntityManager().merge(processor);

        List<Processor> processors = processorService.getProcessorByStatuses(asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED));
        assertThat(processors.size()).isEqualTo(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn("My Processor", "My Processor 3"));
    }

    @Test
    void updateProcessorStatus() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        ProcessorDTO dto = processor.toDTO();
        dto.setStatus(BridgeStatus.AVAILABLE);

        Processor updated = processorService.updateProcessorStatus(dto);
        assertThat(updated.getStatus()).isEqualTo(BridgeStatus.AVAILABLE);
    }

    @Test
    void updateProcessorStatus_bridgeDoesNotExist() {
        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("foo");
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridge(bridge);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    void updateProcessorStatus_processorDoesNotExist() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO();
        processor.setBridge(b.toDTO());
        processor.setId("foo");

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.updateProcessorStatus(processor));
    }

    @Test
    void getProcessor() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        Processor found = processorService.getProcessor(processor.getId(), b.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(processor.getId());
        assertThat(found.getBridge().getId()).isEqualTo(b.getId());
        assertThat(found.getBridge().getCustomerId()).isEqualTo(b.getCustomerId());
    }

    @Test
    void getProcessor_bridgeDoesNotExist() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor(processor.getId(), "doesNotExist", b.getCustomerId()));
    }

    @Test
    void getProcessor_processorDoesNotExist() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessor("doesNotExist", b.getId(), b.getCustomerId()));
    }

    @Test
    void getProcessors() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        ListResult<Processor> results = processorService.getProcessors(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, 0, 100);
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);

        assertThat(results.getItems().get(0).getId()).isEqualTo(processor.getId());
    }

    @Test
    void getProcessors_noProcessorsOnBridge() {

        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ListResult<Processor> results = processorService.getProcessors(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, 0, 100);
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    void getProcessors_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> processorService.getProcessors("doesNotExist", TestConstants.DEFAULT_CUSTOMER_ID, 0, 100));
    }

    @Test
    void testGetProcessorsCount() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        Long result = processorService.getProcessorsCount(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void testDeleteProcessor() {
        Bridge b = createBridge(BridgeStatus.AVAILABLE);
        ProcessorRequest r = new ProcessorRequest("My Processor", createKafkaAction());

        Processor processor = processorService.createProcessor(b.getId(), b.getCustomerId(), r);
        assertThat(processor).isNotNull();

        processorService.deleteProcessor(b.getId(), processor.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        processor = processorService.getProcessor(processor.getId(), b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);
    }
}
