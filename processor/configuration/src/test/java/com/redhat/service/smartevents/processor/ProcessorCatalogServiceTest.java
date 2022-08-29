package com.redhat.service.smartevents.processor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.models.ProcessorCatalogEntry;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ProcessorCatalogServiceTest {

    private static final List<String> availableActions = List.of("kafka_topic_sink_0.1", "send_to_bridge_sink_0.1", "slack_sink_0.1", "webhook_sink_0.1", "aws_lambda_sink_0.1");
    private static final List<String> availableSources = List.of("aws_s3_source_0.1", "aws_sqs_source_0.1", "slack_source_0.1");

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Test
    public void testSchemasAreIncludedInCatalog() {
        File actionsDir = new File("src/main/resources/schemas/actions/");
        File sourcesDir = new File("src/main/resources/schemas/sources/");
        List<String> actions = Arrays.stream(Objects.requireNonNull(actionsDir.listFiles())).map(File::getName).collect(Collectors.toList());
        List<String> sources = Arrays.stream(Objects.requireNonNull(sourcesDir.listFiles())).map(File::getName).collect(Collectors.toList());

        assertThat(actions).contains("catalog.json");
        assertThat(sources).contains("catalog.json");

        assertThat(processorCatalogService.getActionsCatalog())
                .withFailMessage("An action processor json schema file was not added to the catalog.json file.")
                .hasSize(actions.size() - 1);
        assertThat(processorCatalogService.getSourcesCatalog())
                .withFailMessage("A source processor json schema file was not added to the catalog.json file.")
                .hasSize(sources.size() - 1);
    }

    @Test
    public void testSchemasAreAvailable() {
        for (String action : availableActions) {
            assertThat(processorCatalogService.getActionJsonSchema(action)).isNotNull();
        }
        for (String source : availableSources) {
            assertThat(processorCatalogService.getSourceJsonSchema(source)).isNotNull();
        }
    }

    @Test
    public void testValidValidation() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put(SlackAction.CHANNEL_PARAM, "channel")
                .put(SlackAction.WEBHOOK_URL_PARAM, "https://webhook.com");
        ValidationResult result = processorCatalogService.validate("slack_sink_0.1", ProcessorType.SINK, objectNode);
        assertThat(result.getValidationMessages().size()).isEqualTo(0);
    }

    @Test
    public void testInvalidValidation() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put(SlackAction.CHANNEL_PARAM, "channel");
        ValidationResult result = processorCatalogService.validate("slack_sink_0.1", ProcessorType.SINK, objectNode);
        assertThat(result.getValidationMessages()).hasSize(1);
    }

    @Test
    public void testInvalidValidationURL() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put(SlackAction.CHANNEL_PARAM, "channel")
                .put(SlackAction.WEBHOOK_URL_PARAM, "notavalidurl");
        ValidationResult result = processorCatalogService.validate("slack_sink_0.1", ProcessorType.SINK, objectNode);
        assertThat(result.getValidationMessages()).hasSizeGreaterThan(0);
    }

    @ParameterizedTest
    @MethodSource("createActionPasswordProperties")
    public void testActionPasswordProperties(String id, List<String> expectedResult) {
        List<String> actualResult = processorCatalogService.getActionPasswordProperties(id);
        assertThat(actualResult).isEqualTo(expectedResult);

        // test twice to test memoization
        List<String> actualResult2 = processorCatalogService.getActionPasswordProperties(id);
        assertThat(actualResult2).isEqualTo(expectedResult);
    }

    @Test
    public void testActionPasswordPropertiesCompleteness() {
        List<String> definedActionIds = createActionPasswordProperties()
                .map(arg -> (String) arg.get()[0])
                .sorted()
                .collect(Collectors.toList());
        List<String> serviceActionIds = processorCatalogService.getActionsCatalog().stream()
                .map(ProcessorCatalogEntry::getId)
                .sorted()
                .collect(Collectors.toList());
        assertThat(definedActionIds).isEqualTo(serviceActionIds);
    }

    private static Stream<Arguments> createActionPasswordProperties() {
        Object[][] arguments = {
                { "ansible_tower_job_template_sink_0.1", List.of("basic_auth_password") },
                { "aws_lambda_sink_0.1", List.of("aws_access_key", "aws_secret_key") },
                { "google_pubsub_sink_0.1", List.of("gcp_service_account_key") },
                { "kafka_topic_sink_0.1", List.of("kafka_client_secret") },
                { "send_to_bridge_sink_0.1", Collections.emptyList() },
                { "slack_sink_0.1", List.of("slack_webhook_url") },
                { "webhook_sink_0.1", List.of("basic_auth_password") }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("createSourcePasswordProperties")
    public void testSourcePasswordProperties(String id, List<String> expectedResult) {
        List<String> actualResult = processorCatalogService.getSourcePasswordProperties(id);
        assertThat(actualResult).isEqualTo(expectedResult);

        // test twice to test memoization
        List<String> actualResult2 = processorCatalogService.getSourcePasswordProperties(id);
        assertThat(actualResult2).isEqualTo(expectedResult);
    }

    @Test
    public void testSourcePasswordPropertiesCompleteness() {
        List<String> definedSourceIds = createSourcePasswordProperties()
                .map(arg -> (String) arg.get()[0])
                .sorted()
                .collect(Collectors.toList());
        List<String> serviceSourceIds = processorCatalogService.getSourcesCatalog().stream()
                .map(ProcessorCatalogEntry::getId)
                .sorted()
                .collect(Collectors.toList());
        assertThat(definedSourceIds).isEqualTo(serviceSourceIds);
    }

    private static Stream<Arguments> createSourcePasswordProperties() {
        Object[][] arguments = {
                { "aws_s3_source_0.1", List.of("aws_access_key", "aws_secret_key") },
                { "aws_sqs_source_0.1", List.of("aws_access_key", "aws_secret_key") },
                { "azure_eventhubs_source_0.1", List.of("azure_shared_access_key", "azure_blob_access_key") },
                { "google_pubsub_source_0.1", List.of("gcp_service_account_key") },
                { "slack_source_0.1", List.of("slack_token") }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
