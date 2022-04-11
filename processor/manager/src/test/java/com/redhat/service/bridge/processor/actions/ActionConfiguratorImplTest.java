package com.redhat.service.bridge.processor.actions;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionBean;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionValidator;
import com.redhat.service.bridge.processor.actions.sendtobridge.SendToBridgeActionBean;
import com.redhat.service.bridge.processor.actions.sendtobridge.SendToBridgeActionResolver;
import com.redhat.service.bridge.processor.actions.sendtobridge.SendToBridgeActionValidator;
import com.redhat.service.bridge.processor.actions.slack.SlackActionBean;
import com.redhat.service.bridge.processor.actions.slack.SlackActionConnector;
import com.redhat.service.bridge.processor.actions.slack.SlackActionResolver;
import com.redhat.service.bridge.processor.actions.slack.SlackActionValidator;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionBean;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ActionConfiguratorImplTest {

    private static final Map<String, ActionExpectedBeanClasses> EXPECTED_BEANS = Map.of(
            KafkaTopicActionBean.TYPE, expect(KafkaTopicActionValidator.class, null, null),
            SendToBridgeActionBean.TYPE, expect(SendToBridgeActionValidator.class, SendToBridgeActionResolver.class, null),
            SlackActionBean.TYPE, expect(SlackActionValidator.class, SlackActionResolver.class, SlackActionConnector.class),
            WebhookActionBean.TYPE, expect(WebhookActionValidator.class, null, null));

    @Inject
    ActionConfiguratorImpl actionConfigurator;

    @Test
    void testExpectedBeans() {
        for (Map.Entry<String, ActionExpectedBeanClasses> entry : EXPECTED_BEANS.entrySet()) {
            String type = entry.getKey();
            ActionExpectedBeanClasses expected = entry.getValue();

            assertThat(actionConfigurator.getValidator(type))
                    .as("ActionConfigurator.getValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(actionConfigurator.getResolver(type))
                    .as("ActionConfigurator.getResolver(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(actionConfigurator.getConnector(type))
                    .as("ActionConfigurator.getConnector(\"%s\") should not return null", type)
                    .isNotNull();

            assertThat(actionConfigurator.getValidator(type))
                    .as("ActionConfigurator.getValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            if (expected.resolverClass == null) {
                assertThat(actionConfigurator.getResolver(type))
                        .as("ActionConfigurator.getResolver(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(actionConfigurator.getResolver(type))
                        .as("ActionConfigurator.getResolver(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(actionConfigurator.getResolver(type))
                        .as("ActionConfigurator.getResolver(\"%s\") should contain instance of %s", type, expected.resolverClass.getSimpleName())
                        .containsInstanceOf(expected.resolverClass);
            }

            if (expected.connectorClass == null) {
                assertThat(actionConfigurator.getConnector(type))
                        .as("ActionConfigurator.getConnector(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(actionConfigurator.getConnector(type))
                        .as("ActionConfigurator.getConnector(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(actionConfigurator.getConnector(type))
                        .as("ActionConfigurator.getConnector(\"%s\") should contain instance of %s", type, expected.connectorClass.getSimpleName())
                        .containsInstanceOf(expected.connectorClass);
            }
        }
    }

    @Test
    void testUnexpectedBeans() {
        for (ActionValidator validator : actionConfigurator.getValidators()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
        for (ActionResolver resolver : actionConfigurator.getResolvers()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
        for (ActionConnector connector : actionConfigurator.getConnectors()) {
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
