package com.redhat.service.bridge.manager.workers.resources;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.Fixtures;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";

    @InjectMock
    ConnectorWorker connectorWorker;

    @Inject
    WorkManager workManager;

    @Inject
    ProcessorWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PROVISIONING" })
    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = workManager.schedule(processor);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
        assertThat(workManager.exists(work)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = workManager.schedule(processor);

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).handleWork(any(Work.class));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        ArgumentCaptor<Work> workArgumentCaptor = ArgumentCaptor.forClass(Work.class);
        verify(connectorWorker).handleWork(workArgumentCaptor.capture());

        Work connectorWork = workArgumentCaptor.getValue();
        assertThat(connectorWork).isNotNull();
        assertThat(connectorWork.getId()).isEqualTo(work.getId());
        assertThat(connectorWork.getManagedResourceId()).isEqualTo(connectorEntity.getId());
        assertThat(connectorWork.getSubmittedAt()).isEqualTo(work.getSubmittedAt());

        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING, false),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, false));
    }

    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = workManager.schedule(processor);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        assertThat(refreshed.getModifiedAt()).isNotNull();
        assertThat(workManager.exists(work)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = workManager.schedule(processor);

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).handleWork(any(Work.class));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        assertThat(refreshed.getModifiedAt()).isNotNull();

        ArgumentCaptor<Work> workArgumentCaptor = ArgumentCaptor.forClass(Work.class);
        verify(connectorWorker).handleWork(workArgumentCaptor.capture());

        Work connectorWork = workArgumentCaptor.getValue();
        assertThat(connectorWork).isNotNull();
        assertThat(connectorWork.getId()).isEqualTo(work.getId());
        assertThat(connectorWork.getManagedResourceId()).isEqualTo(connectorEntity.getId());
        assertThat(connectorWork.getSubmittedAt()).isEqualTo(work.getSubmittedAt());

        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, false));
    }

}
