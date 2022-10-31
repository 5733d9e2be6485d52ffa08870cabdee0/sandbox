package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsAction;
import com.redhat.service.smartevents.processor.actions.google.GooglePubSubAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.ActionResolver;
import com.redhat.service.smartevents.processor.resolvers.SinkConnectorResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.AnsibleTowerJobTemplateActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.CustomActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.KafkaTopicActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.SendToBridgeActionResolver;
import com.redhat.service.smartevents.processor.validators.ActionValidator;
import com.redhat.service.smartevents.processor.validators.DefaultActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ActionConfiguratorImplTest {

    private static final Map<String, ExpectedBeanClasses> EXPECTED_ACTION_BEANS = Map.of(
            KafkaTopicAction.TYPE, expect(DefaultActionValidator.class, KafkaTopicActionResolver.class),
            SendToBridgeAction.TYPE, expect(DefaultActionValidator.class, SendToBridgeActionResolver.class),
            SlackAction.TYPE, expect(DefaultActionValidator.class, SinkConnectorResolver.class),
            WebhookAction.TYPE, expect(DefaultActionValidator.class, null),
            AwsLambdaAction.TYPE, expect(DefaultActionValidator.class, SinkConnectorResolver.class),
            AnsibleTowerJobTemplateAction.TYPE, expect(DefaultActionValidator.class, AnsibleTowerJobTemplateActionResolver.class),
            GooglePubSubAction.TYPE, expect(DefaultActionValidator.class, SinkConnectorResolver.class),
            AzureEventHubsAction.TYPE, expect(DefaultActionValidator.class, SinkConnectorResolver.class));

    @Inject
    ActionConfiguratorImpl configurator;

    @Test
    void testExpectedActionBeans() {
        for (Map.Entry<String, ExpectedBeanClasses> entry : EXPECTED_ACTION_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses expected = entry.getValue();

            assertThat(configurator.getValidator(type))
                    .as("GatewayConfigurator.getActionValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getValidator(type))
                    .as("GatewayConfigurator.getActionValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            assertThat(configurator.getActionResolver(type))
                    .as("GatewayConfigurator.getActionResolver(\"%s\") should not return null", type)
                    .isNotNull();

            if (expected.resolverClass == null) {
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should contain instance of %s", type, expected.resolverClass.getSimpleName())
                        .containsInstanceOf(expected.resolverClass);
            }
        }
    }

    @Test
    void testUnexpectedActionBeans() {
        for (CustomActionResolver<Action> resolver : configurator.getActionResolvers()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
    }

    private static class ExpectedBeanClasses {
        Class<? extends ActionValidator> validatorClass;
        Class<? extends ActionResolver> resolverClass;
    }

    private static ExpectedBeanClasses expect(
            Class<? extends ActionValidator> validatorClass,
            Class<? extends ActionResolver> resolverClass) {
        ExpectedBeanClasses expected = new ExpectedBeanClasses();
        expected.validatorClass = validatorClass;
        expected.resolverClass = resolverClass;
        return expected;
    }
}
