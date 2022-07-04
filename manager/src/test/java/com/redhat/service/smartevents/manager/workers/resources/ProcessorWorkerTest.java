package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.quartz.SchedulerException;

import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.workers.resources.WorkerTestUtils.makeWork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";

    @InjectMock
    ConnectorWorker connectorWorker;

    @InjectMock
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
        Work work = makeWork(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PREPARING" })
    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
        verify(workManager, never()).reschedule(any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).createDependencies(work, connectorEntity);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        verify(connectorWorker).createDependencies(work, connectorEntity);

        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(work);
    }

    @Transactional
    @Test
    void handleWorkProvisioningWithKnownResourceWithMultipleConnectors() {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(ManagedResourceStatus.ACCEPTED);
        ConnectorEntity connectorEntity1 = Fixtures.createConnector(processor, ManagedResourceStatus.ACCEPTED, ConnectorType.SINK, "test_sink_0.1", "connector1");
        ConnectorEntity connectorEntity2 = Fixtures.createConnector(processor, ManagedResourceStatus.ACCEPTED, ConnectorType.SINK, "test_sink_0.1", "connector2");

        connectorEntity1.setStatus(ManagedResourceStatus.ACCEPTED);
        connectorEntity2.setStatus(ManagedResourceStatus.ACCEPTED);

        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity1);
        connectorsDAO.persist(connectorEntity2);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity1.setStatus(ManagedResourceStatus.READY);
            return connectorEntity1;
        }).when(connectorWorker).createDependencies(work, connectorEntity1);

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity2.setStatus(ManagedResourceStatus.READY);
            return connectorEntity2;
        }).when(connectorWorker).createDependencies(work, connectorEntity2);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);

        ArgumentCaptor<Work> workArgumentCaptor = ArgumentCaptor.forClass(Work.class);
        ArgumentCaptor<ConnectorEntity> connectorEntityArgumentCaptor = ArgumentCaptor.forClass(ConnectorEntity.class);
        verify(connectorWorker, times(2)).createDependencies(workArgumentCaptor.capture(), connectorEntityArgumentCaptor.capture());
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PREPARING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PREPARING, ManagedResourceStatus.PROVISIONING, false),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.PREPARING, ManagedResourceStatus.READY, true),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.FAILED, ManagedResourceStatus.FAILED, true),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.PREPARING, ManagedResourceStatus.PROVISIONING, false));
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETED);
        verify(workManager, never()).reschedule(any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ManagedResourceStatus.ACCEPTED);
        connectorEntity.setStatus(ManagedResourceStatus.ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(dependencyStatusWhenComplete);
            return connectorEntity;
        }).when(connectorWorker).deleteDependencies(work, connectorEntity);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        verify(connectorWorker).deleteDependencies(work, connectorEntity);

        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(any());
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
