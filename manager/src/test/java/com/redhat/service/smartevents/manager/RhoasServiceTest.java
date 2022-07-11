package com.redhat.service.smartevents.manager;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.rhoas.RhoasClient;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class RhoasServiceTest {

    private static final String TEST_OPS_CLIENT_ID = "test-ops-client-id";
    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";

    @Inject
    ResourceNamesProvider resourceNamesProvider;

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
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER));
        verify(rhoasClientMock, times(2)).createTopicAndGrantAccess(any(), eq(TEST_OPS_CLIENT_ID), any());

        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER));
        assertThatNoException()
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER));
        verify(rhoasClientMock, times(2)).deleteTopicAndRevokeAccess(any(), eq(TEST_OPS_CLIENT_ID), any());
    }

    @Test
    void testWithCompletionException() throws InterruptedException {
        CountDownLatch createTopicAndGrantAccessLatch = new CountDownLatch(4);
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenReturn(mockCompletionExceptionWithLatch(createTopicAndGrantAccessLatch, Topic.class));
        CountDownLatch deleteTopicAndRevokeAccessLatch = new CountDownLatch(4);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenReturn(mockCompletionExceptionWithLatch(deleteTopicAndRevokeAccessLatch, Void.class));

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createFailureErrorMessageFor(testProcessorTopicName()));
        assertThat(createTopicAndGrantAccessLatch.await(60, TimeUnit.SECONDS)).isTrue();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteFailureErrorMessageFor(testProcessorTopicName()));
        assertThat(deleteTopicAndRevokeAccessLatch.await(60, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testWithTimeoutException() throws InterruptedException {
        CountDownLatch createTopicAndGrantAccessLatch = new CountDownLatch(4);
        when(rhoasClientMock.createTopicAndGrantAccess(any(), any(), any())).thenReturn(mockTimeoutExceptionWithLatch(createTopicAndGrantAccessLatch, Topic.class));
        CountDownLatch deleteTopicAndRevokeAccessLatch = new CountDownLatch(4);
        when(rhoasClientMock.deleteTopicAndRevokeAccess(any(), any(), any())).thenReturn(mockTimeoutExceptionWithLatch(deleteTopicAndRevokeAccessLatch, Void.class));

        RhoasService testService = buildTestService();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.createTopicAndGrantAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.createTimeoutErrorMessageFor(testProcessorTopicName()));
        assertThat(createTopicAndGrantAccessLatch.await(60, TimeUnit.SECONDS)).isTrue();

        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testBridgeTopicName(), RhoasTopicAccessType.CONSUMER_AND_PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(testBridgeTopicName()));
        assertThatExceptionOfType(InternalPlatformException.class)
                .isThrownBy(() -> testService.deleteTopicAndRevokeAccessFor(testProcessorTopicName(), RhoasTopicAccessType.PRODUCER))
                .withMessage(RhoasServiceImpl.deleteTimeoutErrorMessageFor(testProcessorTopicName()));
        assertThat(deleteTopicAndRevokeAccessLatch.await(60, TimeUnit.SECONDS)).isTrue();
    }

    private RhoasService buildTestService() {
        RhoasServiceImpl service = new RhoasServiceImpl();
        service.rhoasTimeout = 10;
        service.rhoasMaxRetries = 2;
        service.rhoasBackoff = "PT1S";
        service.rhoasJitter = 0.1;
        service.rhoasOpsAccountClientId = TEST_OPS_CLIENT_ID;
        service.rhoasClient = rhoasClientMock;
        return service;
    }

    private String testBridgeTopicName() {
        return resourceNamesProvider.getBridgeTopicName(TEST_BRIDGE_ID);
    }

    private String testProcessorTopicName() {
        return resourceNamesProvider.getProcessorTopicName(TEST_PROCESSOR_ID, "actionName");
    }

    // verify(rhoasClientMock, times(4)).createTopicAndGrantAccess(any(), any(), any()); does not take into account the retries. So this is a workaround.
    private <T> Uni<T> mockCompletionExceptionWithLatch(CountDownLatch latch, Class<T> clazz) {
        return Uni.createFrom().item(() -> {
            latch.countDown();
            throw new CompletionException("Mock exception", new RuntimeException());
        });
    }

    private <T> Uni<T> mockTimeoutExceptionWithLatch(CountDownLatch latch, Class<T> clazz) {
        return Uni.createFrom().item(() -> {
            latch.countDown();
            throw new TimeoutException();
        });
    }
}
