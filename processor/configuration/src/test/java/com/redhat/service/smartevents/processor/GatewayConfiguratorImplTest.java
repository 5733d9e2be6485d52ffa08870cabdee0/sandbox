package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.processor.actions.ActionConnector;
import com.redhat.service.smartevents.processor.actions.ActionResolver;
import com.redhat.service.smartevents.processor.actions.ActionValidator;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicActionValidator;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionResolver;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionValidator;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionConnector;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionResolver;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionValidator;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewayConfiguratorImplTest {

    private static final Map<String, ActionExpectedBeanClasses> EXPECTED_BEANS = Map.of(
            KafkaTopicAction.TYPE, expect(KafkaTopicActionValidator.class, null, null),
            SendToBridgeAction.TYPE, expect(SendToBridgeActionValidator.class, SendToBridgeActionResolver.class, null),
            SlackAction.TYPE, expect(SlackActionValidator.class, SlackActionResolver.class, SlackActionConnector.class),
            WebhookAction.TYPE, expect(WebhookActionValidator.class, null, null));

    @Inject
    GatewayConfiguratorImpl actionConfigurator;

    @Test
    void testExpectedBeans() {
        for (Map.Entry<String, ActionExpectedBeanClasses> entry : EXPECTED_BEANS.entrySet()) {
            String type = entry.getKey();
            ActionExpectedBeanClasses expected = entry.getValue();

            assertThat(actionConfigurator.getActionValidator(type))
                    .as("GatewayConfigurator.getValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(actionConfigurator.getActionResolver(type))
                    .as("GatewayConfigurator.getResolver(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(actionConfigurator.getActionConnector(type))
                    .as("GatewayConfigurator.getConnector(\"%s\") should not return null", type)
                    .isNotNull();

            assertThat(actionConfigurator.getActionValidator(type))
                    .as("GatewayConfigurator.getValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            if (expected.resolverClass == null) {
                assertThat(actionConfigurator.getActionResolver(type))
                        .as("GatewayConfigurator.getResolver(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(actionConfigurator.getActionResolver(type))
                        .as("GatewayConfigurator.getResolver(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(actionConfigurator.getActionResolver(type))
                        .as("GatewayConfigurator.getResolver(\"%s\") should contain instance of %s", type, expected.resolverClass.getSimpleName())
                        .containsInstanceOf(expected.resolverClass);
            }

            if (expected.connectorClass == null) {
                assertThat(actionConfigurator.getActionConnector(type))
                        .as("GatewayConfigurator.getConnector(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(actionConfigurator.getActionConnector(type))
                        .as("GatewayConfigurator.getConnector(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(actionConfigurator.getActionConnector(type))
                        .as("GatewayConfigurator.getConnector(\"%s\") should contain instance of %s", type, expected.connectorClass.getSimpleName())
                        .containsInstanceOf(expected.connectorClass);
            }
        }
    }

    @Test
    void testUnexpectedBeans() {
        for (ActionValidator validator : actionConfigurator.getActionValidators()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
        for (ActionResolver resolver : actionConfigurator.getActionResolvers()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
        for (ActionConnector connector : actionConfigurator.getActionConnectors()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected connector bean for type %s of class %s. Add it to this test.", connector.getType(), connector.getClass())
                    .containsKey(connector.getType());
        }
    }

    private static class ActionExpectedBeanClasses {
        Class<? extends ActionValidator> validatorClass;
        Class<? extends ActionResolver> resolverClass;
        Class<? extends ActionConnector> connectorClass;
    }

    private static ActionExpectedBeanClasses expect(
            Class<? extends ActionValidator> validatorClass,
            Class<? extends ActionResolver> resolverClass,
            Class<? extends ActionConnector> connectorClass) {
        ActionExpectedBeanClasses expected = new ActionExpectedBeanClasses();
        expected.validatorClass = validatorClass;
        expected.resolverClass = resolverClass;
        expected.connectorClass = connectorClass;
        return expected;
    }
}
