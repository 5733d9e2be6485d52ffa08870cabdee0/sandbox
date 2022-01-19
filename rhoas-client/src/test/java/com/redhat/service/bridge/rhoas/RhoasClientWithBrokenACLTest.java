package com.redhat.service.bridge.rhoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasEnabledTestProfile.class)
class RhoasClientWithBrokenACLTest extends RhoasClientTestBase {

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 0, 2);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 0, 3);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 0, 5);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, true, 0, 3, 0, 2);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, true, 0, 5, 0, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenACLDeletion() {
        configureMockAPIWithBrokenACLDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, true, 0, 8, 0, 5);
    }
}
