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
class RhoasClientWithBrokenTopicTest extends RhoasClientTestBase {

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, true, 1, 3, 0, 2);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, true, 1, 5, 0, 3);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 1, 8, 0, 5);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithAlreadyCreatedTopic() {
        configureMockAPIWithAlreadyCreatedTopic();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 1, 8, 0, 0);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 1, 2);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenTopicDeletion() {
        configureMockAPIWithBrokenTopicDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 1, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithAlreadyDeletedTopic() {
        configureMockAPIWithAlreadyDeletedTopic();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 0, 0, 1, 5);
    }
}
