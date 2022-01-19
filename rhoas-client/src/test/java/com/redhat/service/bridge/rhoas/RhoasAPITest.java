package com.redhat.service.bridge.rhoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.RhoasMockServerResource;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

import static com.redhat.service.bridge.test.rhoas.KafkaInstanceAdminMockServerConfigurator.TEST_TOPIC_NAME;
import static com.redhat.service.bridge.test.rhoas.KafkaMgmtV1MockServerConfigurator.TEST_SERVICE_ACCOUNT_NAME;
import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(value = RhoasMockServerResource.class, restrictToAnnotatedClass = true)
@TestProfile(RhoasEnabledTestProfile.class)
class RhoasAPITest extends RhoasTestBase {

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
        configureMockSSO();
    }

    @Test
    void testWithAllWorking() {
        configureMockAPIWithAllWorking();
        given().filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(new RhoasAPI.Request(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .post("/rhoas/topic")
                .then().statusCode(200);
    }

    @Test
    void testWithBrokenTopicCreation() {
        configureMockAPIWithBrokenTopicCreation();
        given().filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(new RhoasAPI.Request(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .post("/rhoas/topic")
                .then().statusCode(500);
    }

    @Test
    void testWithBrokenServiceAccountCreation() {
        configureMockAPIWithBrokenServiceAccountCreation();
        given().filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(new RhoasAPI.Request(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .post("/rhoas/topic")
                .then().statusCode(200);
    }

    @Test
    void testWithBrokenACLCreation() {
        configureMockAPIWithBrokenACLCreation();
        given().filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(new RhoasAPI.Request(TEST_TOPIC_NAME, TEST_SERVICE_ACCOUNT_NAME))
                .post("/rhoas/topic")
                .then().statusCode(500);
    }

}
