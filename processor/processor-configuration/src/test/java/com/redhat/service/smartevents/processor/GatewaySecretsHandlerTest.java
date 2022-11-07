package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;

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

    @Test
    void testAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ENDPOINT,
                WebhookAction.BASIC_AUTH_USERNAME_PARAM, TEST_USERNAME,
                WebhookAction.BASIC_AUTH_PASSWORD_PARAM, TEST_PASSWORD));

        Action maskedAction = secretsHandler.mask(action);

        assertThat(maskedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(maskedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_ENDPOINT);
        assertThat(maskedAction.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(TEST_USERNAME);
        assertThat(maskedAction.getParameters().get(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(emptyObjectNode());
    }

    @Test
    void testSource() {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setMapParameters(Map.of(
                SlackSource.CHANNEL_PARAM, TEST_CHANNEL,
                SlackSource.TOKEN_PARAM, TEST_TOKEN));

        Source maskedAction = secretsHandler.mask(source);

        assertThat(maskedAction.getType()).isEqualTo(SlackSource.TYPE);
        assertThat(maskedAction.getParameter(SlackSource.CHANNEL_PARAM)).isEqualTo(TEST_CHANNEL);
        assertThat(maskedAction.getParameters().get(SlackSource.TOKEN_PARAM)).isEqualTo(emptyObjectNode());
    }

}
