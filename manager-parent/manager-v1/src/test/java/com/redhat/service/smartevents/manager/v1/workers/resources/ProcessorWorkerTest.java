package com.redhat.service.smartevents.manager.v1.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v1.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v1.persistence.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.v1.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v1.utils.Fixtures;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DELETED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DELETING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PREPARING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.v1.workers.resources.WorkerTestUtils.makeWork;
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

    @V1
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
    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(READY);
        verify(workManager, never()).reschedule(any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResourceWithConnector")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete,
            boolean throwsConnectorError) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ACCEPTED);
        connectorEntity.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker failing
            connectorEntity.setStatus(throwsConnectorError ? FAILED : dependencyStatusWhenComplete);
            if (dependencyStatusWhenComplete == FAILED) {
                connectorEntity.setErrorId(1);
                connectorEntity.setErrorUUID(UUID.randomUUID().toString());
            }
            return connectorEntity;
        }).when(connectorWorker).handleWork(work);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        if (dependencyStatusWhenComplete == FAILED) {
            assertThat(refreshed.getErrorId()).isNotNull();
            assertThat(refreshed.getErrorUUID()).isNotNull();
        }

        verify(connectorWorker).handleWork(work);

        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(work);
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(ACCEPTED, PREPARING, READY, true, false),
                Arguments.of(ACCEPTED, FAILED, FAILED, true, false),
                Arguments.of(ACCEPTED, PREPARING, PROVISIONING, false, false),
                Arguments.of(PREPARING, PREPARING, READY, true, false),
                Arguments.of(PREPARING, FAILED, FAILED, true, false),
                Arguments.of(PREPARING, PREPARING, PROVISIONING, false, false),
                Arguments.of(ACCEPTED, FAILED, FAILED, true, true),
                Arguments.of(ACCEPTED, FAILED, FAILED, true, true));
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, READY);
        processor.setStatus(status);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(DELETED);
        verify(workManager, never()).reschedule(any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResourceWithConnector")
    void handleWorkDeletingWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus statusWhenComplete,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean isWorkComplete,
            boolean throwsConnectorError) {
        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, READY);
        processor.setStatus(status);
        ConnectorEntity connectorEntity = Fixtures.createSinkConnector(processor, ACCEPTED);
        connectorEntity.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge);
        processorDAO.persist(processor);
        connectorsDAO.persist(connectorEntity);

        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));

        doAnswer((i) -> {
            //Emulate ConnectorWorker completing work
            connectorEntity.setStatus(throwsConnectorError ? FAILED : dependencyStatusWhenComplete);
            if (dependencyStatusWhenComplete == FAILED) {
                connectorEntity.setErrorId(1);
                connectorEntity.setErrorUUID(UUID.randomUUID().toString());
            }
            return connectorEntity;
        }).when(connectorWorker).handleWork(work);

        Processor refreshed = worker.handleWork(work);

        assertThat(refreshed.getStatus()).isEqualTo(statusWhenComplete);
        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        if (dependencyStatusWhenComplete == FAILED) {
            assertThat(refreshed.getErrorId()).isNotNull();
            assertThat(refreshed.getErrorUUID()).isNotNull();
        }

        verify(connectorWorker).handleWork(work);

        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(any());
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResourceWithConnector() {
        return Stream.of(
                Arguments.of(DEPROVISION, DEPROVISION, DELETED, true, false),
                Arguments.of(DEPROVISION, FAILED, FAILED, true, false),
                Arguments.of(DEPROVISION, DEPROVISION, DELETING, false, false),
                Arguments.of(DELETING, DELETING, DELETED, true, false),
                Arguments.of(DELETING, FAILED, FAILED, true, false),
                Arguments.of(DELETING, DELETING, DELETING, false, false),
                Arguments.of(DEPROVISION, FAILED, FAILED, true, true),
                Arguments.of(DELETING, FAILED, FAILED, true, true));
    }

}
