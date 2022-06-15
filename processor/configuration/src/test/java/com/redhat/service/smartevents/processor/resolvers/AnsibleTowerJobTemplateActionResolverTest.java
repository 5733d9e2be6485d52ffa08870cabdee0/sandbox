package com.redhat.service.smartevents.processor.resolvers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.custom.AnsibleTowerJobTemplateActionResolver;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class AnsibleTowerJobTemplateActionResolverTest {

    private static final String TEST_CSTM_ID = "test-customer";
    private static final String TEST_BRDG_ID = "test-bridge";
    private static final String TEST_PRCS_ID = "test-processor";

    @Inject
    AnsibleTowerJobTemplateActionResolver resolver;

    @Test
    void testActionWithInvalidEndpoint() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "#:",
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, "12");

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        assertThatExceptionOfType(GatewayProviderException.class)
                .isThrownBy(() -> resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID));
    }

    @Test
    void testValidAction() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "https://host:1234",
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, "12");

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        Action resolvedAction = resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID);
        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameters()).hasSize(1);
        assertThat(resolvedAction.hasParameter(WebhookAction.ENDPOINT_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo("https://host:1234/api/v2/job_templates/12/launch/");
    }

    @Test
    void testValidActionWithExtraParameters() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "https://host:1234",
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, "12",
                AnsibleTowerJobTemplateAction.BASIC_AUTH_USERNAME_PARAM, "username",
                AnsibleTowerJobTemplateAction.BASIC_AUTH_PASSWORD_PARAM, "password",
                AnsibleTowerJobTemplateAction.SSL_VERIFICATION_DISABLED, "true");

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        Action resolvedAction = resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID);
        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameters()).hasSize(4);
        assertThat(resolvedAction.hasParameter(WebhookAction.ENDPOINT_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo("https://host:1234/api/v2/job_templates/12/launch/");
        assertThat(resolvedAction.hasParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo("username");
        assertThat(resolvedAction.hasParameter(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo("password");
        assertThat(resolvedAction.hasParameter(WebhookAction.SSL_VERIFICATION_DISABLED)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.SSL_VERIFICATION_DISABLED)).isEqualTo("true");
    }
}
