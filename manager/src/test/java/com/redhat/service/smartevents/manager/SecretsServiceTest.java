package com.redhat.service.smartevents.manager;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import static com.redhat.service.smartevents.manager.SecretsService.emptyObjectNode;
import static org.assertj.core.api.Assertions.assertThat;

class SecretsServiceTest {

    static final String TEST_ENDPOINT = "http://example.com/webhook";
    static final String TEST_USERNAME = "myusername";
    static final String TEST_NEW_USERNAME = "mynewusername";
    static final String TEST_PASSWORD = "mypassword";

    static final String PLAINTEXT_PARAM = "plaintext-param";
    static final String PLAINTEXT_VALUE = "plaintext-value";
    static final String PLAINTEXT_UPDATED = "plaintext-updated";
    static final String SECRET_PARAM = "secret-param";
    static final TextNode SECRET_VALUE = new TextNode("secret-value");
    static final TextNode SECRET_UPDATED = new TextNode("secret-updated");

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testMergeObjectNodes() {
        ObjectNode existingNode = new ObjectNode(JsonNodeFactory.instance, Map.of(
                WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT),
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_USERNAME),
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, emptyObjectNode()));

        ObjectNode newNode = emptyObjectNode();
        newNode.set(WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_NEW_USERNAME));
        newNode.set(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, new TextNode(TEST_PASSWORD));

        ObjectNode merged = SecretsService.mergeObjectNodes(existingNode, newNode, false);

        assertThat(merged.get(WebhookAction.ENDPOINT_PARAM)).isEqualTo(new TextNode(TEST_ENDPOINT));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(new TextNode(TEST_NEW_USERNAME));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(new TextNode(TEST_PASSWORD));
    }

    @Test
    void testMergeObjectNodesWithSecretUnchanged() {
        ObjectNode existingNode = new ObjectNode(JsonNodeFactory.instance, Map.of(
                WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT),
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_USERNAME),
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, new TextNode(TEST_PASSWORD)));

        ObjectNode newNode = emptyObjectNode();
        newNode.set(WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_NEW_USERNAME));
        newNode.set(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, emptyObjectNode());

        ObjectNode merged = SecretsService.mergeObjectNodes(existingNode, newNode, false);

        assertThat(merged.get(WebhookAction.ENDPOINT_PARAM)).isEqualTo(new TextNode(TEST_ENDPOINT));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(new TextNode(TEST_NEW_USERNAME));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(new TextNode(TEST_PASSWORD));
    }

    @Test
    void testMergeObjectNodesWithDeleteMissingExisting() {
        ObjectNode existingNode = new ObjectNode(JsonNodeFactory.instance, Map.of(
                WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT),
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_USERNAME),
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, emptyObjectNode()));

        ObjectNode newNode = emptyObjectNode();
        newNode.set(WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_NEW_USERNAME));
        newNode.set(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, new TextNode(TEST_PASSWORD));

        ObjectNode merged = SecretsService.mergeObjectNodes(existingNode, newNode, true);

        assertThat(merged.has(WebhookAction.ENDPOINT_PARAM)).isFalse();
        assertThat(merged.get(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(new TextNode(TEST_NEW_USERNAME));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(new TextNode(TEST_PASSWORD));
    }

    @Test
    void testMergeObjectNodesWithSecretUnchangedAndDeleteMissingExisting() {
        ObjectNode existingNode = new ObjectNode(JsonNodeFactory.instance, Map.of(
                WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT),
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_USERNAME),
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, new TextNode(TEST_PASSWORD)));

        ObjectNode newNode = emptyObjectNode();
        newNode.set(WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_NEW_USERNAME));
        newNode.set(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, emptyObjectNode());

        ObjectNode merged = SecretsService.mergeObjectNodes(existingNode, newNode, true);

        assertThat(merged.has(WebhookAction.ENDPOINT_PARAM)).isFalse();
        assertThat(merged.get(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(new TextNode(TEST_NEW_USERNAME));
        assertThat(merged.get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(new TextNode(TEST_PASSWORD));
    }

    @Test
    void testEmptyObjectNode() {
        assertThat(emptyObjectNode())
                .isNotNull()
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("createTestMergeProcessorDefinitionsParams")
    void testMergeProcessorDefinitions(ProcessorDefinition existingDefinition, ProcessorDefinition updatedDefinition, ProcessorDefinition expectedDefinition) {
        ProcessorDefinition mergedDefinition = SecretsService.mergeProcessorDefinitions(existingDefinition, updatedDefinition);
        assertThat(mergedDefinition)
                .withFailMessage(() -> {
                    try {
                        return "SecretsService::mergeProcessorDefinition failure\n" +
                                "Merge between\n" +
                                "    existing: " + MAPPER.writeValueAsString(existingDefinition) + "\n" +
                                "    updated : " + MAPPER.writeValueAsString(updatedDefinition) + "\n" +
                                "should have produced\n" +
                                "    expected: " + MAPPER.writeValueAsString(expectedDefinition) + "\n" +
                                "but instead produced\n" +
                                "    actual  : " + MAPPER.writeValueAsString(mergedDefinition);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .isEqualTo(expectedDefinition);
    }

    static Stream<Arguments> createTestMergeProcessorDefinitionsParams() {
        return Stream.of(
                createTestMergeProcessorDefinitionsParamsStream(null, null, null, null),
                createTestMergeProcessorDefinitionsParamsStream(null, null, emptyObjectNode(), null),
                createTestMergeProcessorDefinitionsParamsStream(null, null, SECRET_UPDATED, SECRET_UPDATED),
                createTestMergeProcessorDefinitionsParamsStream(null, PLAINTEXT_UPDATED, null, null),
                createTestMergeProcessorDefinitionsParamsStream(null, PLAINTEXT_UPDATED, emptyObjectNode(), null),
                createTestMergeProcessorDefinitionsParamsStream(null, PLAINTEXT_UPDATED, SECRET_UPDATED, SECRET_UPDATED),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), null, null, null),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), null, emptyObjectNode(), emptyObjectNode()),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), null, SECRET_UPDATED, SECRET_UPDATED),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), PLAINTEXT_UPDATED, null, null),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), PLAINTEXT_UPDATED, emptyObjectNode(), emptyObjectNode()),
                createTestMergeProcessorDefinitionsParamsStream(emptyObjectNode(), PLAINTEXT_UPDATED, SECRET_UPDATED, SECRET_UPDATED),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, null, null, null),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, null, emptyObjectNode(), SECRET_VALUE),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, null, SECRET_UPDATED, SECRET_UPDATED),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, PLAINTEXT_UPDATED, null, null),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, PLAINTEXT_UPDATED, emptyObjectNode(), SECRET_VALUE),
                createTestMergeProcessorDefinitionsParamsStream(SECRET_VALUE, PLAINTEXT_UPDATED, SECRET_UPDATED, SECRET_UPDATED))
                .flatMap(Function.identity())
                .map(Arguments::of);
    }

    static Stream<Object[]> createTestMergeProcessorDefinitionsParamsStream(JsonNode currentSecretValue, String updatedPlaintextValue, JsonNode updatedSecretValue, JsonNode expectedSecretValue) {
        Action existingAction = createTestGateway(Action::new, PLAINTEXT_PARAM, currentSecretValue);
        Action updatedAction = createTestGateway(Action::new, updatedPlaintextValue, updatedSecretValue);
        Action expectedAction = createTestGateway(Action::new, updatedPlaintextValue, expectedSecretValue);

        Source existingSource = createTestGateway(Source::new, PLAINTEXT_PARAM, currentSecretValue);
        Source updatedSource = createTestGateway(Source::new, updatedPlaintextValue, updatedSecretValue);
        Source expectedSource = createTestGateway(Source::new, updatedPlaintextValue, expectedSecretValue);

        Object[][] arguments = {
                createTestProcessorDefinitionArray(true, true, true, false, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(true, false, true, true, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(true, false, true, false, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(false, false, false, true, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(false, true, false, false, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(false, true, false, true, existingAction, updatedAction, expectedAction),
                createTestProcessorDefinitionArray(true, true, true, false, existingSource, updatedSource, expectedSource),
                createTestProcessorDefinitionArray(true, false, true, true, existingSource, updatedSource, expectedSource),
                createTestProcessorDefinitionArray(true, false, true, false, existingSource, updatedSource, expectedSource),
                createTestProcessorDefinitionArray(false, false, false, true, existingSource, updatedSource, expectedSource),
                createTestProcessorDefinitionArray(false, true, false, false, existingSource, updatedSource, expectedSource),
                createTestProcessorDefinitionArray(false, true, false, true, existingSource, updatedSource, expectedSource),
        };

        return Arrays.stream(arguments);
    }

    static <T extends Gateway> ProcessorDefinition[] createTestProcessorDefinitionArray(boolean existingFilters, boolean updatedFilters, boolean existingTemplate, boolean updatedTemplate,
            T existingGateway, T updatedGateway, T expectedGateway) {
        final Set<BaseFilter> existingFiltersValue = existingFilters ? Set.of(new StringEquals("key", "value")) : null;
        final Set<BaseFilter> updatedFiltersValue = updatedFilters ? Set.of(new StringEquals("key", "value")) : null;
        final String existingTemplateValue = existingTemplate ? "template" : null;
        final String updatedTemplateValue = updatedTemplate ? "template" : null;

        return new ProcessorDefinition[] {
                createTestDefinition(existingFiltersValue, existingTemplateValue, existingGateway),
                createTestDefinition(updatedFiltersValue, updatedTemplateValue, updatedGateway),
                createTestDefinition(updatedFiltersValue, updatedTemplateValue, expectedGateway)
        };
    }

    static ProcessorDefinition createTestDefinition(Set<BaseFilter> filters, String transformationTemplate, Gateway gateway) {
        if (gateway instanceof Source) {
            return new ProcessorDefinition(filters, transformationTemplate, (Source) gateway, createTestGateway(Action::new));
        }
        return new ProcessorDefinition(filters, transformationTemplate, (Action) gateway, (Action) gateway);
    }

    static <T extends Gateway> T createTestGateway(Supplier<T> constructor) {
        return createTestGateway(constructor, PLAINTEXT_VALUE, emptyObjectNode());
    }

    static <T extends Gateway> T createTestGateway(Supplier<T> constructor, String plaintextValue, JsonNode secretValue) {
        ObjectNode parameters = emptyObjectNode();
        if (plaintextValue != null) {
            parameters.set(PLAINTEXT_PARAM, new TextNode(plaintextValue));
        }
        if (secretValue != null) {
            parameters.set(SECRET_PARAM, secretValue);
        }

        T gateway = constructor.get();
        gateway.setType("test-type");
        gateway.setParameters(parameters);
        return gateway;
    }

}
