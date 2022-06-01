package com.redhat.service.smartevents.processor;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationResult;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class JsonSchemaServiceTest {

    private static final List<String> availableActions = List.of("kafka_topic_sink_0.1", "send_to_bridge_sink_0.1", "slack_sink_0.1", "webhook_sink_0.1");
    private static final List<String> availableSources = List.of("aws_s3_source_0.1", "aws_sqs_source_0.1", "slack_source_0.1");

    @Inject
    JsonSchemaService jsonSchemaService;

    @Test
    public void testSchemasAreIncludedInCatalog() {
        File actionsDir = new File("src/main/resources/schemas/actions/");
        File sourcesDir = new File("src/main/resources/schemas/sources/");
        List<String> actions = Arrays.stream(Objects.requireNonNull(actionsDir.listFiles())).map(File::getName).collect(Collectors.toList());
        List<String> sources = Arrays.stream(Objects.requireNonNull(sourcesDir.listFiles())).map(File::getName).collect(Collectors.toList());

        assertThat(actions).contains("catalog.json");
        assertThat(sources).contains("catalog.json");

        assertThat(jsonSchemaService.getActionsCatalog().size())
                .withFailMessage("An action processor json schema file was not added to the catalog.json file.")
                .isEqualTo(actions.size() - 1);
        assertThat(jsonSchemaService.getSourcesCatalog().size())
                .withFailMessage("A source processor json schema file was not added to the catalog.json file.")
                .isEqualTo(sources.size() - 1);
    }

    @Test
    public void testSchemasAreAvailable(){
        for (String action : availableActions){
            assertThat(jsonSchemaService.getActionJsonSchema(action)).isNotNull();
        }
        for (String source : availableSources){
            assertThat(jsonSchemaService.getSourceJsonSchema(source)).isNotNull();
        }
    }

    @Test
    public void testValidValidation(){
        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put(SlackAction.CHANNEL_PARAM, "channel")
                .put(SlackAction.WEBHOOK_URL_PARAM, "url");
        ValidationResult result = jsonSchemaService.validate("slack_sink_0.1", ProcessorType.SINK, objectNode);
        assertThat(result.getValidationMessages().size()).isEqualTo(0);
    }

    @Test
    public void testInvalidValidation(){
        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put(SlackAction.CHANNEL_PARAM, "channel");
        ValidationResult result = jsonSchemaService.validate("slack_sink_0.1", ProcessorType.SINK, objectNode);
        assertThat(result.getValidationMessages().size()).isEqualTo(1);
    }
}
