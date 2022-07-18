package com.redhat.service.smartevents.manager.api.user;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorCatalogResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorSchemaEntryResponse;
import com.redhat.service.smartevents.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;

import static com.redhat.service.smartevents.infra.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SchemaAPITest {

    private static final List<String> availableActions =
            List.of("kafka_topic_sink_0.1", "send_to_bridge_sink_0.1", "slack_sink_0.1", "webhook_sink_0.1", "aws_lambda_sink_0.1", "ansible_tower_job_template_sink_0.1", "google_pubsub_sink_0.1");
    private static final List<String> availableSources = List.of("aws_s3_source_0.1", "aws_sqs_source_0.1", "slack_source_0.1", "google_pubsub_source_0.1");

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    public void cleanUp() {
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testAuthentication() {
        TestUtils.getProcessorsSchemaCatalog().then().statusCode(401);
        TestUtils.getSourceProcessorsSchema("slack_source_0.1").then().statusCode(401);
        TestUtils.getActionProcessorsSchema("slack_sink_0.1").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors() {
        ProcessorCatalogResponse catalog = TestUtils.getProcessorsSchemaCatalog().as(ProcessorCatalogResponse.class);

        assertThat(catalog.getItems()).isNotNull();
        assertThat(catalog.getItems())
                .withFailMessage("The size of the catalog does not match. If you added a new action or a new source under /resources/schemas/ please update this test")
                .hasSize(12);
        for (ProcessorSchemaEntryResponse entry : catalog.getItems()) {
            switch (entry.getType()) {
                case "action":
                    assertThat(availableActions).contains(entry.getId());
                    break;
                case "source":
                    assertThat(availableSources).contains(entry.getId());
                    break;
                default:
                    fail("entry type does not match 'source' nor 'action'");
            }
            assertThatNoException().isThrownBy(() -> new URI(entry.getHref())); // is a valid URI
            assertThat(entry.getName()).isNotNull().isNotBlank();
            assertThat(entry.getDescription()).isNotNull().isNotBlank();
            assertThat(entry.getHref()).contains(entry.getId()); // The href should contain the name
            TestUtils.jsonRequest().get(entry.getHref()).then().statusCode(200);
        }
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getUnexistingProcessorsSchema() {
        TestUtils.getSourceProcessorsSchema("wrong").then().statusCode(404);
        TestUtils.getSourceProcessorsSchema("kafka_topic_sink_0.1").then().statusCode(404);
        TestUtils.getActionProcessorsSchema("aws_s3_source_0.1").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessorsSchema() {
        for (String source : availableSources) {
            JsonNode schema = TestUtils.getSourceProcessorsSchema(source).as(JsonNode.class);
            assertThat(schema.get("required") != null || schema.get("optional") != null).isTrue();
            assertThat(schema.get("type")).isNotNull();
            assertThat(schema.get("properties")).isNotNull();
        }

        for (String action : availableActions) {
            JsonNode schema = TestUtils.getActionProcessorsSchema(action).as(JsonNode.class);
            assertThat(schema.get("required") != null || schema.get("optional") != null).isTrue();
            assertThat(schema.get("type")).isNotNull();
            assertThat(schema.get("properties")).isNotNull();
        }
    }
}
