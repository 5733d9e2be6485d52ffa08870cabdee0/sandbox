package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dns.DnsService;
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
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DELETED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DELETING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
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
    DnsService dnsServiceMock;

    @InjectMock
    ResourceNamesProvider resourceNamesProviderMock;

    @InjectMock
    ProcessorService processorServiceMock;

    @InjectMock
    WorkManager workManagerMock;

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
        when(processorServiceMock.getErrorHandler(anyString(), anyString())).thenReturn(Optional.empty());
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
            boolean throwDnsError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("rhoas error"));
        }

        if (throwDnsError) {
            when(dnsServiceMock.createDnsRecord(any())).thenThrow(new InternalPlatformException("dns error"));
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).createDnsRecord(eq(bridge.getId()));
        verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(any());

        if (throwRhoasError) {
            assertThat(refreshed.getBridgeErrorId()).isNotNull();
            assertThat(refreshed.getBridgeErrorUUID()).isNotNull();
        }
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParams")
    void testProvisionWorkWithKnownResourceAndErrorHandlerNotPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                throwDnsError,
                isWorkComplete);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParamsWithErrorHandler")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresent(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete,
            ManagedResourceStatus errorHandlerStatus) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                throwDnsError,
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
            boolean throwDnsError,
            boolean isWorkComplete,
            ManagedResourceStatus errorHandlerStatus) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                throwDnsError,
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

    @Transactional
    @ParameterizedTest
    @MethodSource("provisionWorkWithKnownResourceParamsWithErrorHandler")
    void testProvisionWorkWithKnownResourceAndErrorHandlerPresentMultipleRetries(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete,
            ManagedResourceStatus errorHandlerStatus) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                throwDnsError,
                isWorkComplete,
                true,
                errorHandlerStatus);

        Bridge bridge = bridgeDAO.findById(TestConstants.DEFAULT_BRIDGE_ID);

        Work work = WorkerTestUtils.makeWork(bridge);

        // There should be no further interactions with ProcessorService during re-tries
        worker.handleWork(work);

        verify(processorServiceMock,
                never()).updateErrorHandlerProcessor(eq(bridge.getId()),
                        anyString(),
                        eq(bridge.getCustomerId()),
                        any(ProcessorRequest.class));
    }

    private void doTestProvisionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete) {
        doTestProvisionWorkWithKnownResourceAndErrorHandler(status,
                dependencyStatusWhenComplete,
                throwRhoasError,
                throwDnsError,
                isWorkComplete,
                false,
                READY);
    }

    private void doTestProvisionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete,
            boolean errorHandlerProcessorPresent,
            ManagedResourceStatus errorHandlerStatus) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction(), createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        Processor errorHandler = createErrorHandlerProcessor(bridge, errorHandlerStatus);
        if (errorHandlerProcessorPresent && dependencyStatusWhenComplete == FAILED && errorHandlerStatus == FAILED) {
            errorHandler.setBridgeErrorId(1);
            errorHandler.setBridgeErrorUUID("uuid");
        }

        when(processorServiceMock.getErrorHandler(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(errorHandlerProcessorPresent
                        ? Optional.of(errorHandler)
                        : Optional.empty());

        if (throwRhoasError) {
            when(rhoasServiceMock.createTopicAndGrantAccessFor(any(), any()))
                    .thenThrow(new InternalPlatformException("rhoas error"));
        }

        if (throwDnsError) {
            when(dnsServiceMock.createDnsRecord(any())).thenThrow(new InternalPlatformException("dns error"));

        }

        Bridge refreshed = worker.handleWork(work);

        verify(rhoasServiceMock)
                .createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(rhoasServiceMock, times(throwRhoasError ? 0 : 1))
                .createTopicAndGrantAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(processorServiceMock, times(throwRhoasError || errorHandlerProcessorPresent ? 0 : 1))
                .createErrorHandlerProcessor(eq(bridge.getId()), eq(TestConstants.DEFAULT_CUSTOMER_ID), eq(TestConstants.DEFAULT_USER_NAME), any());
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).createDnsRecord(eq(bridge.getId()));

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(work);

        if (throwRhoasError || throwDnsError) {
            // An error occurred provisioning the Bridge
            assertThat(refreshed.getBridgeErrorId()).isNotNull();
            assertThat(refreshed.getBridgeErrorUUID()).isNotNull();
        } else {
            if (errorHandlerStatus == FAILED) {
                assertThat(refreshed.getBridgeErrorId()).isNotNull();
                assertThat(refreshed.getBridgeErrorUUID()).isNotNull();
            }
            // An error occurred provisioning the Bridge's Error Handler. Check details were propagated.
            assertThat(refreshed.getBridgeErrorId()).isEqualTo(errorHandler.getBridgeErrorId());
            assertThat(refreshed.getBridgeErrorUUID()).isEqualTo(errorHandler.getBridgeErrorUUID());
        }
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(ACCEPTED, READY, false, false, true),
                Arguments.of(ACCEPTED, PROVISIONING, true, false, false),
                Arguments.of(ACCEPTED, PROVISIONING, false, true, false),
                Arguments.of(ACCEPTED, PROVISIONING, true, true, false),
                Arguments.of(PREPARING, READY, false, false, true),
                Arguments.of(PREPARING, PROVISIONING, true, false, false),
                Arguments.of(PREPARING, PROVISIONING, false, true, false),
                Arguments.of(PREPARING, PROVISIONING, true, true, false));
    }

    private static Stream<Arguments> provisionWorkWithKnownResourceParamsWithErrorHandler() {
        return Stream.of(
                Arguments.of(ACCEPTED, READY, false, false, true, READY),
                Arguments.of(ACCEPTED, PROVISIONING, true, false, false, READY),
                Arguments.of(PREPARING, READY, false, false, true, READY),
                Arguments.of(PREPARING, PROVISIONING, true, false, false, READY),
                Arguments.of(ACCEPTED, FAILED, false, false, true, FAILED),
                Arguments.of(ACCEPTED, PROVISIONING, true, false, false, FAILED),
                Arguments.of(PREPARING, FAILED, false, false, true, FAILED),
                Arguments.of(PREPARING, PROVISIONING, true, false, false, FAILED));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParams")
    void testDeletionWorkWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        when(processorServiceMock.getErrorHandler(any(), any())).thenReturn(Optional.empty());

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("rhoas error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(eq(TEST_TOPIC_NAME), any());
        }

        if (throwDnsError) {
            doThrow(new InternalPlatformException("dns error")).when(dnsServiceMock).deleteDnsRecord(eq(bridge.getId()));
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).deleteDnsRecord(eq(bridge.getId()));
        verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(work);

        if (throwRhoasError) {
            assertThat(refreshed.getBridgeErrorId()).isNotNull();
            assertThat(refreshed.getBridgeErrorUUID()).isNotNull();
        }
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("deletionWorkWithKnownResourceParamsWithErrorHandler")
    void testDeletionWorkWithKnownResourceAndErrorHandler(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhoasError,
            boolean throwDnsError,
            boolean isWorkComplete,
            boolean isErrorHandlerDeleted) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(TestConstants.DEFAULT_BRIDGE_ID);
        bridge.setStatus(status);
        bridge.setDefinition(new BridgeDefinition(createWebhookAction(), createWebhookAction()));
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge);

        // The ErrorHandler Processor will first exist
        Processor errorHandler = createErrorHandlerProcessor(bridge, READY);
        when(processorServiceMock.getErrorHandler(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .thenReturn(Optional.of(errorHandler));

        // The ErrorHandler Processor may then be successfully deleted
        if (isErrorHandlerDeleted) {
            when(processorServiceMock.getErrorHandler(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                    .thenReturn(Optional.empty());
        }

        if (throwRhoasError) {
            doThrow(new InternalPlatformException("rhoas error")).when(rhoasServiceMock).deleteTopicAndRevokeAccessFor(any(), any());
        }

        if (throwDnsError) {
            doThrow(new InternalPlatformException("dns error")).when(dnsServiceMock).deleteDnsRecord(any());
        }

        Bridge refreshed1 = worker.handleWork(work);

        verify(rhoasServiceMock).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(rhoasServiceMock, times(throwRhoasError ? 0 : 1)).deleteTopicAndRevokeAccessFor(TEST_ERROR_HANDLER_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        verify(dnsServiceMock, times(throwRhoasError ? 0 : 1)).deleteDnsRecord(eq(bridge.getId()));

        if (isErrorHandlerDeleted) {
            assertThat(refreshed1.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
            verify(workManagerMock, times(isWorkComplete ? 0 : 1)).reschedule(work);
        } else {
            assertThat(refreshed1.getDependencyStatus()).isEqualTo(DELETING);

            errorHandler.setStatus(FAILED);
            errorHandler.setBridgeErrorId(1);
            errorHandler.setBridgeErrorUUID(UUID.randomUUID().toString());

            Bridge refreshed2 = worker.handleWork(work);

            assertThat(refreshed2.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
            verify(workManagerMock, times(isWorkComplete ? 1 : 2)).reschedule(work);
        }

        if (throwRhoasError || throwDnsError) {
            // An error occurred provisioning the Bridge
            assertThat(refreshed1.getBridgeErrorId()).isNotNull();
            assertThat(refreshed1.getBridgeErrorUUID()).isNotNull();
        } else if (!isErrorHandlerDeleted) {
            assertThat(refreshed1.getBridgeErrorId()).isNotNull();
            assertThat(refreshed1.getBridgeErrorUUID()).isNotNull();

            // An error occurred provisioning the Bridge's Error Handler. Check details were propagated.
            assertThat(refreshed1.getBridgeErrorId()).isEqualTo(errorHandler.getBridgeErrorId());
            assertThat(refreshed1.getBridgeErrorUUID()).isEqualTo(errorHandler.getBridgeErrorUUID());
        }
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParams() {
        return Stream.of(
                Arguments.of(DEPROVISION, DELETED, false, false, true),
                Arguments.of(DEPROVISION, DELETING, false, true, false),
                Arguments.of(DEPROVISION, DELETING, true, false, false),
                Arguments.of(DEPROVISION, DELETING, true, true, false),
                Arguments.of(DELETING, DELETED, false, false, true),
                Arguments.of(DELETING, DELETING, false, true, false),
                Arguments.of(DELETING, DELETING, true, false, false),
                Arguments.of(DELETING, DELETING, true, true, false));
    }

    private static Stream<Arguments> deletionWorkWithKnownResourceParamsWithErrorHandler() {
        return Stream.of(
                Arguments.of(DEPROVISION, DELETED, false, false, true, true),
                Arguments.of(DEPROVISION, DELETING, true, false, false, true),
                Arguments.of(DELETING, DELETED, false, false, true, true),
                Arguments.of(DELETING, DELETING, true, false, false, true),
                Arguments.of(DEPROVISION, FAILED, false, false, true, false),
                Arguments.of(DEPROVISION, DELETING, true, false, false, false),
                Arguments.of(DELETING, FAILED, false, false, true, false),
                Arguments.of(DELETING, DELETING, true, false, false, false));
    }

    private static Processor createErrorHandlerProcessor(Bridge bridge, ManagedResourceStatus status) {
        Processor processor = Fixtures.createProcessor(bridge, status);
        processor.setType(ProcessorType.ERROR_HANDLER);
        return processor;
    }

}
