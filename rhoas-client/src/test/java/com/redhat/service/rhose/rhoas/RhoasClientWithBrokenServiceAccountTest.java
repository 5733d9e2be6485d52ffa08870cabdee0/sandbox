package com.redhat.service.rhose.rhoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.rhose.test.rhoas.RhoasMockServerResource;
import com.redhat.service.rhose.test.rhoas.testprofiles.RhoasTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasTestProfile.class)
class RhoasClientWithBrokenServiceAccountTest extends RhoasClientTestBase {

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER, false, 1, 3, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessProducerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.PRODUCER, false, 1, 5, 0, 0);
    }

    @Test
    void testCreateTopicAndGrantAccessConsumerAndProducerWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        testCreateTopicAndGrantAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 1, 8, 0, 0);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER, false, 0, 0, 1, 2);
    }

    @Test
    void testDeleteTopicAndRevokeAccessProducerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.PRODUCER, false, 0, 0, 1, 3);
    }

    @Test
    void testDeleteTopicAndRevokeAccessConsumerAndProducerWithBrokenServiceAccountDeletion() {
        configureMockAPIWithBrokenServiceAccountDeletion();
        testDeleteTopicAndRevokeAccess(RhoasTopicAccessType.CONSUMER_AND_PRODUCER, false, 0, 0, 1, 5);
    }
}
