package com.redhat.service.smartevents.manager.workers.resources;

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
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.utils.TestUtils.createWebhookAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Inject
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
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerNotPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        doTestProvisionworkWithKnownResourceAndErrorHandler(status, dependencyStatusWhenComplete, throwRhoasError, isWorkComplete, false);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        doTestProvisionworkWithKnownResourceAndErrorHandler(status, dependencyStatusWhenComplete, throwRhoasError, isWorkComplete, true);
    }

    private void doTestProvisionworkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            boolean errorHandlerProcessorPresent) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);

        List<Processor> processors = errorHandlerProcessorPresent
                ? List.of(createErrorHandlerProcessor(bridge))
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
        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING, true, false),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.PREPARING, ManagedResourceStatus.PROVISIONING, true, false));
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

        Work work = workManager.schedule(bridge);

        when(processorServiceMock.getHiddenProcessors(any(), any()))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(eq(TEST_TOPIC_NAME), any());
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParams")
    void testDeletionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(List.of(createErrorHandlerProcessor(bridge)), 0, 1));

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isNotEqualTo(dependencyStatusWhenComplete);
        assertThat(workManager.exists(work)).isTrue();

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(any(), any());

            Bridge refreshed2 = worker.handleWork(work);

            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            verify(rhoasServiceMock, never()).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        } else {
            Bridge refreshed2 = worker.handleWork(work);

            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        }

        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false));
    }

    private static Processor createErrorHandlerProcessor(Bridge bridge) {
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.READY);
        processor.setType(ProcessorType.ERROR_HANDLER);
        return processor;
    }

}
