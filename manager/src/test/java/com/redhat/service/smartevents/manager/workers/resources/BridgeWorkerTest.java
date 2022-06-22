package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PREPARING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.utils.TestUtils.createWebhookAction;
import static com.redhat.service.smartevents.manager.workers.resources.WorkerTestUtils.makeWork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class BridgeWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";
    private static final String TEST_TOPIC_NAME = "TopicName";
    private static final String TEST_ERROR_HANDLER_TOPIC_NAME = "ErrorHandlerTopicName";

    @InjectMock
    RhoasService rhoasServiceMock;

    @InjectMock
    ResourceNamesProvider resourceNamesProviderMock;

    @InjectMock
    ProcessorService processorServiceMock;

    @InjectMock
    WorkManager workManager;

    @Inject
    BridgeWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUp();
        when(resourceNamesProviderMock.getBridgeTopicName(any())).thenReturn(TEST_TOPIC_NAME);
        when(resourceNamesProviderMock.getBridgeErrorTopicName(any())).thenReturn(TEST_ERROR_HANDLER_TOPIC_NAME);
        when(processorServiceMock.getHiddenProcessors(anyString(), anyString())).thenReturn(new ListResult<>(Collections.emptyList()));
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = makeWork(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerNotPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                isWorkComplete);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParamsWithErrorHandler")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            ManagedResourceStatus errorHandlerStatus) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                isWorkComplete,
                true,
                errorHandlerStatus);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParamsWithErrorHandler")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresentUpdating(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            ManagedResourceStatus errorHandlerStatus) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                isWorkComplete,
                true,
                errorHandlerStatus);

        Bridge bridge = bridgeDAO.findById(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(ACCEPTED);
        bridge.setGeneration(bridge.getGeneration() + 1);

        Work work = WorkerTestUtils.makeWork(bridge);

        worker.handleWork(work);

        verify(processorServiceMock,
                times(throwRhoasError ? 0 : 1)).updateErrorHandlerProcessor(eq(bridge.getId()),
                        anyString(),
                        eq(bridge.getCustomerId()),
                        any(ProcessorRequest.class));
    }

    private void doTestProvisionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                isWorkComplete,
                false,
                READY);
    }

    private void doTestProvisionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            boolean errorHandlerProcessorPresent,
            ManagedResourceStatus errorHandlerStatus) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        List<Processor> processors = errorHandlerProcessorPresent
                ? List.of(createErrorHandlerProcessor(bridge, errorHandlerStatus))
                : Collections.emptyList();

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(processors, 0, processors.size()));

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any()))
                    .thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(work);

        verify(rhoasServiceMock)
                .createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(rhoasServiceMock, times(throwRhoasError ? 0 : 1))
                .createTopicAndGrantAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(processorServiceMock, times(throwRhoasError || errorHandlerProcessorPresent ? 0 : 1))
                .createErrorHandlerProcessor(eq(bridge.getId()), eq(TestConstants.DEFAULT_CUSTOMER_ID), eq(TestConstants.DEFAULT_USER_NAME), any());

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(work);
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(ACCEPTED, READY, false, true),
                Arguments.of(ACCEPTED, PROVISIONING, true, false),
                Arguments.of(PREPARING, READY, false, true),
                Arguments.of(PREPARING, PROVISIONING, true, false));
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParamsWithErrorHandler() {
        return Stream.of(
                Arguments.of(ACCEPTED, READY, false, true, READY),
                Arguments.of(ACCEPTED, PROVISIONING, true, false, READY),
                Arguments.of(PREPARING, READY, false, true, READY),
                Arguments.of(PREPARING, PROVISIONING, true, false, READY));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParams")
    void testDeletionWorkWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        when(processorServiceMock.getHiddenProcessors(any(), any()))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(eq(TEST_TOPIC_NAME), any());
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(work);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParamsWithErrorHandler")
    void testDeletionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            boolean isErrorHandlerDeleted) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        // The ErrorHandler Processor will first exist
        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(List.of(createErrorHandlerProcessor(bridge, READY))));

        // The ErrorHandler Processor may then be successfully deleted
        if (isErrorHandlerDeleted) {
            when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                    .thenReturn(new ListResult<>(Collections.emptyList()));
        } else {
            when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                    .thenReturn(new ListResult<>(List.of(createErrorHandlerProcessor(bridge, READY))));
        }

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(any(), any());
        }

        Bridge refreshed1 = worker.handleWork(work);

        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(rhoasServiceMock, times(throwRhoasError ? 0 : 1)).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        assertThat(refreshed1.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);

        if (isErrorHandlerDeleted) {
            assertThat(refreshed1.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
            verify(workManager, times(isWorkComplete ? 0 : 1)).reschedule(work);
        } else {
            assertThat(refreshed1.getDependencyStatus()).isEqualTo(ManagedResourceStatus.DELETING);

            Bridge refreshed2 = worker.handleWork(work);

            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
            verify(workManager, times(isWorkComplete ? 1 : 2)).reschedule(work);
        }
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false));
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParamsWithErrorHandler() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, false, true, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, false, true, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, false, false, false),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, false, false, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false, false));
    }

    private static Processor createErrorHandlerProcessor(Bridge bridge, ManagedResourceStatus status) {
        Processor processor = Fixtures.createProcessor(bridge, status);
        processor.setType(ProcessorType.ERROR_HANDLER);
        return processor;
    }

}
