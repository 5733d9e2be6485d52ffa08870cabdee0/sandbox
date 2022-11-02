package com.redhat.service.smartevents.processor.actions.webhook;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.infra.core.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.BASIC_AUTH_PASSWORD_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.BASIC_AUTH_USERNAME_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.ENDPOINT_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.SSL_VERIFICATION_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class WebhookActionInvokerBuilderTest {

    public static final String TEST_ENDPOINT = "http://www.example.com/webhook";
    public static final String TEST_USERNAME = "username";
    public static final String TEST_PASSWORD = "password";

    @Inject
    WebhookActionInvokerBuilder builder;

    @Test
    void testInvoker() {
        ProcessorDTO processor = createProcessor();

        ActionInvoker actionInvoker = builder.build(processor, processor.getDefinition().getResolvedAction());
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(WebhookActionInvoker.class);

        WebhookActionInvoker webhookActionInvoker = (WebhookActionInvoker) actionInvoker;
        assertThat(webhookActionInvoker.getEndpoint()).isEqualTo(TEST_ENDPOINT);
        assertThat(webhookActionInvoker.getWebClient()).isNotNull();
        assertThat(webhookActionInvoker.getOidcClient()).isNull();
        assertThat(webhookActionInvoker.getBasicAuthUsername()).isNull();
        assertThat(webhookActionInvoker.getBasicAuthPassword()).isNull();
    }

    @Test
    void testInvokerWithBasicAuth() {
        ProcessorDTO processor = createProcessor();
        processor.getDefinition().getResolvedAction().getParameters().put(BASIC_AUTH_USERNAME_PARAM, TEST_USERNAME);
        processor.getDefinition().getResolvedAction().getParameters().put(BASIC_AUTH_PASSWORD_PARAM, TEST_PASSWORD);

        ActionInvoker actionInvoker = builder.build(processor, processor.getDefinition().getResolvedAction());
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(WebhookActionInvoker.class);

        WebhookActionInvoker webhookActionInvoker = (WebhookActionInvoker) actionInvoker;
        assertThat(webhookActionInvoker.getEndpoint()).isEqualTo(TEST_ENDPOINT);
        assertThat(webhookActionInvoker.getWebClient()).isNotNull();
        assertThat(webhookActionInvoker.getOidcClient()).isNull();
        assertThat(webhookActionInvoker.getBasicAuthUsername()).isEqualTo(TEST_USERNAME);
        assertThat(webhookActionInvoker.getBasicAuthPassword()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void testInvokerWithSslVerificationDisabled() {
        ProcessorDTO processor = createProcessor();
        processor.getDefinition().getResolvedAction().getParameters().put(SSL_VERIFICATION_DISABLED, "true");

        ActionInvoker actionInvoker = builder.build(processor, processor.getDefinition().getResolvedAction());
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(WebhookActionInvoker.class);

        WebhookActionInvoker webhookActionInvoker = (WebhookActionInvoker) actionInvoker;
        assertThat(webhookActionInvoker.getEndpoint()).isEqualTo(TEST_ENDPOINT);
        assertThat(webhookActionInvoker.getWebClient()).isNotNull();
        assertThat(webhookActionInvoker.getOidcClient()).isNull();
        assertThat(webhookActionInvoker.getBasicAuthUsername()).isNull();
        assertThat(webhookActionInvoker.getBasicAuthPassword()).isNull();
    }

    private ProcessorDTO createProcessor() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(ENDPOINT_PARAM, TEST_ENDPOINT);
        action.setMapParameters(params);

        ProcessorDTO processor = new ProcessorDTO();
        processor.setType(ProcessorType.SINK);
        processor.setId("myProcessor");
        processor.setDefinition(new ProcessorDefinition(null, null, action));
        processor.setBridgeId("myBridge");

        return processor;
    }
}
