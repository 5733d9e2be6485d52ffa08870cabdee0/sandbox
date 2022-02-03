package com.redhat.service.bridge.manager;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.rhoas.RhoasClient;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

import static com.redhat.service.bridge.manager.models.Bridge.TOPIC_PREFIX;
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
    private static final String TEST_BRIDGE_TOPIC_NAME = TOPIC_PREFIX + TEST_BRIDGE_ID;
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_TOPIC_NAME = TOPIC_PREFIX + TEST_PROCESSOR_ID;

    private static final CompletionException COMPLETION_EXCEPTION = new CompletionException("Mock exception", new RuntimeException());
    private static final TimeoutException TIMEOUT_EXCEPTION = new TimeoutException();

    @InjectMock
    RhoasClient rhoasClientMock;

    @BeforeEach
    void beforeEach() {
        reset(rhoasClientMock);
    }

    @Test
    void testWithAllWorking() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), eq(TEST_OPS_CLIENT_ID), any())).thenReturn(Uni.createFrom().item(Topic::new));
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), eq(TEST_OPS_CLIENT_ID), any())).thenReturn(Uni.createFrom().voidItem());

        RhoasService testService = buildTestService();

        assertThatNoException()
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER));
    }

    @Test
    void testWithCompletionException() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenThrow(COMPLETION_EXCEPTION);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenThrow(COMPLETION_EXCEPTION);

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(TEST_BRIDGE_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(TEST_BRIDGE_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(TEST_PROCESSOR_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(TEST_PROCESSOR_TOPIC_NAME));
    }

    @Test
    void testWithTimeoutException() {
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenThrow(TIMEOUT_EXCEPTION);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenThrow(TIMEOUT_EXCEPTION);

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(TEST_BRIDGE_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_BRIDGE_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(TEST_BRIDGE_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(TEST_PROCESSOR_TOPIC_NAME));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(TEST_PROCESSOR_TOPIC_NAME, RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(TEST_PROCESSOR_TOPIC_NAME));
    }

    private RhoasService buildTestService() {
        RhoasServiceImpl service = new RhoasServiceImpl();
        service.rhoasTimeout = 10;
        service.rhoasOpsAccountClientId = TEST_OPS_CLIENT_ID;
        service.rhoasClient = rhoasClientMock;
        return service;
    }
}
