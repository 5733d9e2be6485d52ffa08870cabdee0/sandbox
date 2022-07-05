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
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

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
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.utils.TestUtils.createWebhookAction;
import static com.redhat.service.smartevents.manager.workers.resources.WorkerTestUtils.makeJobExecutionContext;
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

    @InjectMock(convertScopes = true)
    Scheduler quartzMock;

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
        JobExecutionContext context = makeJobExecutionContext(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(context)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        JobExecutionContext context = makeJobExecutionContext(bridge);

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(context);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(quartzMock, times(isWorkComplete ? 0 : 1)).rescheduleJob(any(), any());
    }

    @Transactional
    protected void persist(Bridge bridge) {
        bridgeDAO.persist(bridge);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerNotPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) throws SchedulerException {
        doTestProvisionworkWithKnownResourceAndErrorHandler(status, dependencyStatusWhenComplete, throwRhoasError, isWorkComplete, false);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) throws SchedulerException {
        doTestProvisionworkWithKnownResourceAndErrorHandler(status, dependencyStatusWhenComplete, throwRhoasError, isWorkComplete, true);
    }

    private void doTestProvisionworkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete,
            boolean errorHandlerProcessorPresent) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        JobExecutionContext context = makeJobExecutionContext(bridge);

        List<Processor> processors = errorHandlerProcessorPresent
                ? List.of(createErrorHandlerProcessor(bridge))
                : Collections.emptyList();

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(processors, 0, processors.size()));

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any()))
                    .thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(context);

        verify(rhoasServiceMock)
                .createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(rhoasServiceMock, times(throwRhoasError ? 0 : 1))
                .createTopicAndGrantAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(processorServiceMock, times(throwRhoasError || errorHandlerProcessorPresent ? 0 : 1))
                .createErrorHandlerProcessor(eq(bridge.getId()), eq(TestConstants.DEFAULT_CUSTOMER_ID), eq(TestConstants.DEFAULT_USER_NAME), any());

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(quartzMock, times(isWorkComplete ? 0 : 1)).rescheduleJob(any(), any());
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
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        JobExecutionContext context = makeJobExecutionContext(bridge);

        when(processorServiceMock.getHiddenProcessors(any(), any()))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(eq(TEST_TOPIC_NAME), any());
        }

        Bridge refreshed = worker.handleWork(context);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(quartzMock, times(isWorkComplete ? 0 : 1)).rescheduleJob(any(), any());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParams")
    void testDeletionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean isWorkComplete) throws SchedulerException {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction()));
        bridgeDAO.persist(bridge);

        JobExecutionContext context = makeJobExecutionContext(bridge);

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(List.of(createErrorHandlerProcessor(bridge)), 0, 1));

        Bridge refreshed = worker.handleWork(context);

        assertThat(refreshed.getDependencyStatus()).isNotEqualTo(dependencyStatusWhenComplete);
        verify(quartzMock).rescheduleJob(any(), any());

        when(processorServiceMock.getHiddenProcessors(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(new ListResult<>(Collections.emptyList(), 0, 0));

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(any(), any());

            Bridge refreshed2 = worker.handleWork(context);

            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            verify(rhoasServiceMock, never()).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        } else {
            Bridge refreshed2 = worker.handleWork(context);

            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        }

        verify(quartzMock, times(isWorkComplete ? 1 : 2)).rescheduleJob(any(), any());
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
