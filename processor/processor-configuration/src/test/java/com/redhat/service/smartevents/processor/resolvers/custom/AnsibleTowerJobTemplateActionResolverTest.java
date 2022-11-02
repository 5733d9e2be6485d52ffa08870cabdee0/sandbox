package com.redhat.service.smartevents.processor.resolvers.custom;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class AnsibleTowerJobTemplateActionResolverTest {

    private static final String TEST_CSTM_ID = "test-customer";
    private static final String TEST_BRDG_ID = "test-bridge";
    private static final String TEST_PRCS_ID = "test-processor";
    private static final String TEST_JOB_TEMPLATE_ID = "12";
    private static final String TEST_ENDPOINT = "https://host:1234";
    private static final String TEST_RESOLVED_ENDPOINT = TEST_ENDPOINT + "/api/v2/job_templates/" + TEST_JOB_TEMPLATE_ID + "/launch/";
    private static final String TEST_USERNAME = "username";
    private static final String TEST_PASSWORD = "password";
    private static final String TRUE = "true";

    @Inject
    AnsibleTowerJobTemplateActionResolver resolver;

    @Test
    void testActionWithInvalidEndpoint() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, "#:",
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, TEST_JOB_TEMPLATE_ID);

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        assertThatExceptionOfType(GatewayProviderException.class)
                .isThrownBy(() -> resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID));
    }

    @Test
    void testValidAction() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, TEST_ENDPOINT,
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, TEST_JOB_TEMPLATE_ID);

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        Action resolvedAction = resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID);
        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameters()).hasSize(1);
        assertThat(resolvedAction.hasParameter(WebhookAction.ENDPOINT_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_RESOLVED_ENDPOINT);
    }

    @Test
    void testValidActionWithExtraParameters() {
        Map<String, String> parameters = Map.of(
                AnsibleTowerJobTemplateAction.ENDPOINT_PARAM, TEST_ENDPOINT,
                AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM, TEST_JOB_TEMPLATE_ID,
                AnsibleTowerJobTemplateAction.BASIC_AUTH_USERNAME_PARAM, TEST_USERNAME,
                AnsibleTowerJobTemplateAction.BASIC_AUTH_PASSWORD_PARAM, TEST_PASSWORD,
                AnsibleTowerJobTemplateAction.SSL_VERIFICATION_DISABLED, TRUE);

        Action inputAction = new Action();
        inputAction.setType(AnsibleTowerJobTemplateAction.TYPE);
        inputAction.setMapParameters(parameters);

        Action resolvedAction = resolver.resolve(inputAction, TEST_CSTM_ID, TEST_BRDG_ID, TEST_PRCS_ID);
        assertThat(resolvedAction).isNotNull();
        assertThat(resolvedAction.getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(resolvedAction.getParameters()).hasSize(4);
        assertThat(resolvedAction.hasParameter(WebhookAction.ENDPOINT_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.ENDPOINT_PARAM)).isEqualTo(TEST_RESOLVED_ENDPOINT);
        assertThat(resolvedAction.hasParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.BASIC_AUTH_USERNAME_PARAM)).isEqualTo(TEST_USERNAME);
        assertThat(resolvedAction.hasParameter(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.BASIC_AUTH_PASSWORD_PARAM)).isEqualTo(TEST_PASSWORD);
        assertThat(resolvedAction.hasParameter(WebhookAction.SSL_VERIFICATION_DISABLED)).isTrue();
        assertThat(resolvedAction.getParameter(WebhookAction.SSL_VERIFICATION_DISABLED)).isEqualTo(TRUE);
    }
}
