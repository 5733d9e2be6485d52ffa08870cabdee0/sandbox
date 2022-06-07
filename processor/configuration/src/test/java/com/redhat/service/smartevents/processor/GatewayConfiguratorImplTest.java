package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsAction;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsActionResolver;
import com.redhat.service.smartevents.processor.actions.eventhubs.AzureEventHubsActionValidator;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicActionValidator;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionResolver;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionValidator;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionResolver;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionValidator;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookActionValidator;
import com.redhat.service.smartevents.processor.sources.SourceResolver;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3Source;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3SourceValidator;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;
import com.redhat.service.smartevents.processor.sources.slack.SlackSourceValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewayConfiguratorImplTest {

    private static final Map<String, ExpectedBeanClasses<Action>> EXPECTED_ACTION_BEANS = Map.of(
            KafkaTopicAction.TYPE, expect(KafkaTopicActionValidator.class, null),
            SendToBridgeAction.TYPE, expect(SendToBridgeActionValidator.class, SendToBridgeActionResolver.class),
            SlackAction.TYPE, expect(SlackActionValidator.class, SlackActionResolver.class),
            WebhookAction.TYPE, expect(WebhookActionValidator.class, null),
            AzureEventHubsAction.TYPE, expect(AzureEventHubsActionValidator.class, AzureEventHubsActionResolver.class));

    private static final Map<String, ExpectedBeanClasses<Source>> EXPECTED_SOURCE_BEANS = Map.of(
            AwsS3Source.TYPE, expect(AwsS3SourceValidator.class, SourceResolver.class),
            AwsSqsSource.TYPE, expect(AwsSqsSourceValidator.class, SourceResolver.class),
            SlackSource.TYPE, expect(SlackSourceValidator.class, SourceResolver.class));

    @Inject
    GatewayConfiguratorImpl configurator;

    @Test
    void testExpectedActionBeans() {
        for (Map.Entry<String, ExpectedBeanClasses<Action>> entry : EXPECTED_ACTION_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses<Action> expected = entry.getValue();

            assertThat(configurator.getActionValidator(type))
                    .as("GatewayConfigurator.getActionValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getActionValidator(type))
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
        for (GatewayValidator<Action> validator : configurator.getActionValidators()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
        for (GatewayResolver<Action> resolver : configurator.getActionResolvers()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
    }

    @Test
    void testExpectedSourceBeans() {
        for (Map.Entry<String, ExpectedBeanClasses<Source>> entry : EXPECTED_SOURCE_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses<Source> expected = entry.getValue();

            assertThat(configurator.getSourceValidator(type))
                    .as("GatewayConfigurator.getSourceValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getSourceValidator(type))
                    .as("GatewayConfigurator.getSourceValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.resolverClass);
        }
    }

    @Test
    void testUnexpectedSourceBeans() {
        for (GatewayValidator<Source> validator : configurator.getSourceValidators()) {
            assertThat(EXPECTED_SOURCE_BEANS)
                    .as("Found unexpected source validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
    }

    private static class ExpectedBeanClasses<T extends Gateway> {
        Class<? extends GatewayValidator<T>> validatorClass;
        Class<? extends GatewayResolver<T>> resolverClass;
    }

    private static <T extends Gateway> ExpectedBeanClasses<T> expect(
            Class<? extends GatewayValidator<T>> validatorClass,
            Class<? extends GatewayResolver<T>> resolverClass) {
        ExpectedBeanClasses<T> expected = new ExpectedBeanClasses<>();
        expected.validatorClass = validatorClass;
        expected.resolverClass = resolverClass;
        return expected;
    }
}
