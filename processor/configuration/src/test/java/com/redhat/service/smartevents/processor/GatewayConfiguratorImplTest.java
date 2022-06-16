package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.resolvers.SourceConnectorResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.AnsibleTowerJobTemplateActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.AwsLambdaActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.SendToBridgeActionResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.SlackActionResolver;
import com.redhat.service.smartevents.processor.sources.aws.AwsS3Source;
import com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;
import com.redhat.service.smartevents.processor.validators.DefaultGatewayValidator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;
import com.redhat.service.smartevents.processor.validators.custom.AnsibleTowerJobTemplateActionValidator;
import com.redhat.service.smartevents.processor.validators.custom.AwsSqsSourceValidator;
import com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewayConfiguratorImplTest {

    private static final Map<String, ExpectedBeanClasses<Action>> EXPECTED_ACTION_BEANS = Map.of(
            KafkaTopicAction.TYPE, expect(DefaultGatewayValidator.class, null),
            SendToBridgeAction.TYPE, expect(DefaultGatewayValidator.class, SendToBridgeActionResolver.class),
            SlackAction.TYPE, expect(DefaultGatewayValidator.class, SlackActionResolver.class),
            WebhookAction.TYPE, expect(WebhookActionValidator.class, null),
            AwsLambdaAction.TYPE, expect(DefaultGatewayValidator.class, AwsLambdaActionResolver.class),
            AnsibleTowerJobTemplateAction.TYPE, expect(AnsibleTowerJobTemplateActionValidator.class, AnsibleTowerJobTemplateActionResolver.class));

    private static final Map<String, ExpectedBeanClasses<Source>> EXPECTED_SOURCE_BEANS = Map.of(
            AwsS3Source.TYPE, expect(DefaultGatewayValidator.class, SourceConnectorResolver.class),
            AwsSqsSource.TYPE, expect(AwsSqsSourceValidator.class, SourceConnectorResolver.class),
            SlackSource.TYPE, expect(DefaultGatewayValidator.class, SourceConnectorResolver.class));

    @Inject
    GatewayConfiguratorImpl configurator;

    @Test
    void testExpectedActionBeans() {
        for (Map.Entry<String, ExpectedBeanClasses<Action>> entry : EXPECTED_ACTION_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses<Action> expected = entry.getValue();

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

            assertThat(configurator.getValidator(type))
                    .as("GatewayConfigurator.getValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getValidator(type))
                    .as("GatewayConfigurator.getValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.resolverClass);
        }
    }

    private static class ExpectedBeanClasses<T extends Gateway> {
        Class<? extends GatewayValidator> validatorClass;
        Class<? extends GatewayResolver<T>> resolverClass;
    }

    private static <T extends Gateway> ExpectedBeanClasses<T> expect(
            Class<? extends GatewayValidator> validatorClass,
            Class<? extends GatewayResolver<T>> resolverClass) {
        ExpectedBeanClasses<T> expected = new ExpectedBeanClasses<>();
        expected.validatorClass = validatorClass;
        expected.resolverClass = resolverClass;
        return expected;
    }
}
