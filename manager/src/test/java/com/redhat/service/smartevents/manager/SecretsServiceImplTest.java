package com.redhat.service.smartevents.manager;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.SecretsService.emptyObjectNode;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_ENDPOINT;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_PASSWORD;
import static com.redhat.service.smartevents.manager.SecretsServiceTest.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SecretsServiceImplTest {

    static final String TEST_CHANNEL = "mychannel";
    static final String TEST_TOKEN = "mytoken";

    @Inject
    SecretsServiceImpl secretsService;

    @Test
    void testMaskAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ENDPOINT,
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, TEST_USERNAME,
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, TEST_PASSWORD));

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
}
