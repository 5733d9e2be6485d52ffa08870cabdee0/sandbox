package com.redhat.service.bridge.rhoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasTestProfile.class)
class RhoasClientWithAllWorkingTest extends RhoasClientTestBase {

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
        configureMockAPIWithAllWorking();
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerOk() {
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, false, 1, 3, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerOk() {
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, false, 1, 5, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerOk() {
        configureMockAPIWithAllWorking();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 1, 8, 0, 0);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, false, 0, 0, 1, 2);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, false, 0, 0, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerOk() {
        configureMockAPIWithAllWorking();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 0, 0, 1, 5);
    }
}
