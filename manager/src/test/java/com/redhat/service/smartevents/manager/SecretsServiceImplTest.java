package com.redhat.service.smartevents.manager;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.EventBridgeSecret;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.vault.VaultService;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;

import static com.redhat.service.smartevents.manager.SecretsService.emptyObjectNode;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_ENDPOINT;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_PASSWORD;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class SecretsServiceImplTest {

    static final String TEST_CHANNEL = "mychannel";
    static final String TEST_TOKEN = "mytoken";

    @Inject
    SecretsServiceImpl secretsService;

    @InjectMock
    VaultService vaultServiceMock;

    @BeforeEach
    public void beforeEach() {
        reset(vaultServiceMock);
    }

    @Test
    void testMaskProcessorWithSimpleWebhookAction() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        testMaskProcessor(false, initialDefinition, false, expectedDefinition, never(), never());
    }

    @Test
    void testMaskProcessorWithSimpleWebhookActionAndInitialHasSecrets() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        testMaskProcessor(true, initialDefinition, false, expectedDefinition, never(), times(1));
    }

    @Test
    void testMaskProcessorWithUnmaskedWebhookAction() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testUnmaskedWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testMaskedWebhookAction());
        testMaskProcessor(false, initialDefinition, true, expectedDefinition, times(1), never());
    }

    @Test
    void testMaskProcessorWithUnmaskedSlackSource() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testUnmaskedSlackSource(), testUnmaskedWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testMaskedSlackSource(), testMaskedWebhookAction());
        testMaskProcessor(false, initialDefinition, true, expectedDefinition, times(1), never());
    }

    void testMaskProcessor(
            boolean initialHasSecrets, ProcessorDefinition initialDefinition,
            boolean expectedHasSecrets, ProcessorDefinition expectedDefinition,
            VerificationMode expectedCreateInvocations, VerificationMode expectedDeleteInvocations) {
        Processor processor = new Processor();
        processor.setId("test-id");
        processor.setHasSecrets(initialHasSecrets);
        processor.setDefinition(initialDefinition);

        secretsService.maskProcessor(processor);

        verify(vaultServiceMock, expectedCreateInvocations).createOrReplace(any());
        verify(vaultServiceMock, expectedDeleteInvocations).delete(any());

        assertThat(processor.hasSecrets()).isEqualTo(expectedHasSecrets);
        assertThat(processor.getDefinition()).isEqualTo(expectedDefinition);
    }

    @Test
    void testGetUnmaskedProcessorDefinitionWithSimpleWebhookAction() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testSimpleWebhookAction());
        testGetUnmaskedProcessorDefinition(false, initialDefinition, expectedDefinition, never());
    }

    @Test
    void testGetUnmaskedProcessorDefinitionWithUnmaskedWebhookAction() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testMaskedWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testUnmaskedWebhookAction());
        testGetUnmaskedProcessorDefinition(true, initialDefinition, expectedDefinition, times(1));
    }

    @Test
    void testGetUnmaskedProcessorDefinitionWithUnmaskedSlackSource() {
        ProcessorDefinition initialDefinition = new ProcessorDefinition(null, null, testMaskedSlackSource(), testMaskedWebhookAction());
        ProcessorDefinition expectedDefinition = new ProcessorDefinition(null, null, testUnmaskedSlackSource(), testUnmaskedWebhookAction());
        testGetUnmaskedProcessorDefinition(true, initialDefinition, expectedDefinition, times(1));
    }

    void testGetUnmaskedProcessorDefinition(
            boolean initialHasSecrets, ProcessorDefinition initialDefinition,
            ProcessorDefinition expectedDefinition, VerificationMode expectedGetInvocations) {
        EventBridgeSecret mockSecret = new EventBridgeSecret("test-secret", Map.of(
                "requestedAction", "{\"" + WebhookAction.BASIC_AUTH_PASSWORD_PARAM + "\":\"" + TEST_PASSWORD + "\"}",
                "requestedSource", "{\"" + SlackSource.TOKEN_PARAM + "\":\"" + TEST_TOKEN + "\"}",
                "resolvedAction", "{\"" + WebhookAction.BASIC_AUTH_PASSWORD_PARAM + "\":\"" + TEST_PASSWORD + "\"}"));
        when(vaultServiceMock.get(any())).thenReturn(Uni.createFrom().item(mockSecret));

        Processor processor = new Processor();
        processor.setId("test-id");
        processor.setHasSecrets(initialHasSecrets);
        processor.setDefinition(initialDefinition);

        ProcessorDefinition actualDefinition = secretsService.getUnmaskedProcessorDefinition(processor);

        verify(vaultServiceMock, expectedGetInvocations).get(any());

        assertThat(actualDefinition).isEqualTo(expectedDefinition);
        assertThat(actualDefinition).isNotSameAs(expectedDefinition);
    }

    @Test
    void testMaskAction() {
        Action action = testUnmaskedWebhookAction();

        ObjectNode secrets = secretsService.maskGateway(action);

        assertThat(action.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(action.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_ENDPOINT);
        assertThat(action.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(TEST_USERNAME);
        assertThat(action.getParameters().get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(emptyObjectNode());

        assertThat(secrets).hasSize(1);
        assertThat(secrets.has(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isTrue();
        assertThat(secrets.get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(new TextNode(TEST_PASSWORD));
    }

    @Test
    void testMaskSource() {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setMapParameters(Map.of(
                SlackSource.CHANNEL_PARAM, TEST_CHANNEL,
                SlackSource.TOKEN_PARAM, TEST_TOKEN));

        ObjectNode secrets = secretsService.maskGateway(source);

        assertThat(source.getType()).isEqualTo(SlackSource.TYPE);
        assertThat(source.getParameter(SlackSource.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL);
        assertThat(source.getParameters().get(SlackSource.TOKEN_PARAM)).isEqualTo(emptyObjectNode());

        assertThat(secrets).hasSize(1);
        assertThat(secrets.has(SlackSource.TOKEN_PARAM)).isTrue();
        assertThat(secrets.get(SlackSource.TOKEN_PARAM)).isEqualTo(new TextNode(TEST_TOKEN));
    }

    Action testSimpleWebhookAction() {
        return testWebhookAction(null, null);
    }

    Action testUnmaskedWebhookAction() {
        return testWebhookAction(TEST_USERNAME, new TextNode(TEST_PASSWORD));
    }

    Action testMaskedWebhookAction() {
        return testWebhookAction(TEST_USERNAME, emptyObjectNode());
    }

    Action testWebhookAction(String username, JsonNode password) {
        ObjectNode parameters = emptyObjectNode();
        parameters.set(WebhookAction.ENDPOINT_PARAM, new TextNode(TEST_ENDPOINT));
        if (username != null) {
            parameters.set(WebhookAction.BASIC_AUTH_USERNAME_PARAM, new TextNode(username));
        }
        if (password != null) {
            parameters.set(WebhookAction.BASIC_AUTH_PASSWORD_PARAM, password);
        }

        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setParameters(parameters);
        return action;
    }

    Source testUnmaskedSlackSource() {
        return testSlackSource(new TextNode(TEST_TOKEN));
    }

    Source testMaskedSlackSource() {
        return testSlackSource(emptyObjectNode());
    }

    Source testSlackSource(JsonNode token) {
        ObjectNode parameters = emptyObjectNode();
        parameters.set(SlackSource.CHANNEL_PARAM, new TextNode(TEST_CHANNEL));
        parameters.set(SlackSource.TOKEN_PARAM, token);

        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setParameters(parameters);
        return source;
    }
}
