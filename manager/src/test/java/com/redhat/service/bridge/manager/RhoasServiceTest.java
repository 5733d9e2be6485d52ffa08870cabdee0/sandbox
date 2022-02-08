package com.redhat.service.bridge.manager;

import java.util.concurrent.CompletionException;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.rhoas.RhoasClient;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class RhoasServiceTest {

    private static final String TEST_OPS_CLIENT_ID = "test-ops-client-id";
    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";

    private static final CompletionException COMPLETION_EXCEPTION = new CompletionException("Mock exception", new RuntimeException());
    private static final TimeoutException TIMEOUT_EXCEPTION = new TimeoutException();

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @InjectMock
    RhoasClient rhoasClientMock;

    @BeforeEach
    void beforeEach() {
        reset(rhoasClientMock);
    }

    @Test
    void testHappyPath() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), eq(TEST_OPS_CLIENT_ID), any())).thenReturn(Uni.createFrom().item(Topic::new));
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), eq(TEST_OPS_CLIENT_ID), any())).thenReturn(Uni.createFrom().voidItem());

        RhoasService testService = buildTestService();

        assertThatNoException()
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER));
    }

    @Test
    void testWithCompletionException() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenThrow(COMPLETION_EXCEPTION);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenThrow(COMPLETION_EXCEPTION);

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(testProcessorTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(testProcessorTopicName()));
    }

    @Test
    void testWithTimeoutException() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenThrow(TIMEOUT_EXCEPTION);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenThrow(TIMEOUT_EXCEPTION);

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(testProcessorTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(testProcessorTopicName()));
    }

    private RhoasService buildTestService() {
        RhoasServiceImpl service = new RhoasServiceImpl();
        service.rhoasTimeout = 10;
        service.rhoasOpsAccountClientId = TEST_OPS_CLIENT_ID;
        service.rhoasClient = rhoasClientMock;
        return service;
    }

    private String testBridgeTopicName() {
        return internalKafkaConfigurationProvider.getTopicPrefix() + TEST_BRIDGE_ID;
    }

    private String testProcessorTopicName() {
        return internalKafkaConfigurationProvider.getTopicPrefix() + TEST_PROCESSOR_ID;
    }
}
