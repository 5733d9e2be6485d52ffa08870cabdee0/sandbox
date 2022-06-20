package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.processor.GatewaySecretsHandler.emptyObjectNode;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewaySecretsHandlerTest {

    private static final String TEST_ENDPOINT = "http://example.com/webhook";
    private static final String TEST_USERNAME = "myusername";
    private static final String TEST_PASSWORD = "mypassword";

    private static final String TEST_CHANNEL = "mychannel";
    private static final String TEST_TOKEN = "mytoken";

    @Inject
    GatewaySecretsHandler secretsHandler;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @Test
    void testMaskAction() throws JsonProcessingException {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ENDPOINT,
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, TEST_USERNAME,
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, TEST_PASSWORD));

        Pair<Gateway, Map<String, String>> maskOutputPair = secretsHandler.mask(action);

        Action maskedAction = (Action) maskOutputPair.getLeft();
        assertThat(maskedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(maskedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_ENDPOINT);
        assertThat(maskedAction.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(TEST_USERNAME);
        assertThat(maskedAction.getParameters().get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(emptyObjectNode());

        Map<String, String> secretValues = maskOutputPair.getRight();
        assertThat(secretValues)
                .hasSize(1)
                .containsEntry(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "\"" + TEST_PASSWORD + "\"");
    }

    @Test
    void testMaskSource() throws JsonProcessingException {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setMapParameters(Map.of(
                SlackSource.CHANNEL_PARAM, TEST_CHANNEL,
                SlackSource.TOKEN_PARAM, TEST_TOKEN));

        Pair<Gateway, Map<String, String>> maskOutputPair = secretsHandler.mask(source);

        Source maskedSource = (Source) maskOutputPair.getLeft();
        assertThat(maskedSource.getType()).isEqualTo(SlackSource.TYPE);
        assertThat(maskedSource.getParameter(SlackSource.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL);
        assertThat(maskedSource.getParameters().get(SlackSource.TOKEN_PARAM)).isEqualTo(emptyObjectNode());

        Map<String, String> secretValues = maskOutputPair.getRight();
        assertThat(secretValues)
                .hasSize(1)
                .containsEntry(SlackSource.TOKEN_PARAM, "\"" + TEST_TOKEN + "\"");
    }

    @Test
    void testUnmaskAction() throws JsonProcessingException {
        Action maskedAction = new Action();
        maskedAction.setType(WebhookAction.TYPE);
        maskedAction.setParameters((new ObjectNode(JsonNodeFactory.instance, Map.of(
                WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT),
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(TEST_USERNAME),
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, emptyObjectNode()))));

        Map<String, String> secrets = Map.of(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, "\"" + TEST_PASSWORD + "\"");

        Action unmaskedAction = (Action) secretsHandler.unmask(maskedAction, secrets);

        assertThat(unmaskedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(unmaskedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_ENDPOINT);
        assertThat(unmaskedAction.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(TEST_USERNAME);
        assertThat(unmaskedAction.getParameter(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void testUnmaskSource() throws JsonProcessingException {
        Source maskedSource = new Source();
        maskedSource.setType(SlackSource.TYPE);
        maskedSource.setParameters(new ObjectNode(JsonNodeFactory.instance, Map.of(
                SlackSource.CHANNEL_PARAM, new TextNode(TEST_CHANNEL),
                SlackSource.TOKEN_PARAM, emptyObjectNode())));

        Map<String, String> secrets = Map.of(SlackSource.TOKEN_PARAM, "\"" + TEST_TOKEN + "\"");

        Source unaskedSource = (Source) secretsHandler.unmask(maskedSource, secrets);

        assertThat(unaskedSource.getType()).isEqualTo(SlackSource.TYPE);
        assertThat(unaskedSource.getParameter(SlackSource.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL);
        assertThat(unaskedSource.getParameter(SlackSource.TOKEN_PARAM)).isEqualTo(TEST_TOKEN);
    }

}
