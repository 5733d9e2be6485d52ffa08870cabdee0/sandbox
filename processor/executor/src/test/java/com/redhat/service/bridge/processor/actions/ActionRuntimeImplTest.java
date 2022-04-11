package com.redhat.service.bridge.processor.actions;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionBean;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionInvokerBuilder;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionBean;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionInvokerBuilder;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ActionRuntimeImplTest {

    private static final Map<String, Class<? extends ActionInvokerBuilder>> EXPECTED_BEANS = Map.of(
            KafkaTopicActionBean.TYPE, KafkaTopicActionInvokerBuilder.class,
            WebhookActionBean.TYPE, WebhookActionInvokerBuilder.class);

    @Inject
    ActionRuntimeImpl actionRuntime;

    @Test
    void testExpectedBeans() {
        for (Map.Entry<String, Class<? extends ActionInvokerBuilder>> entry : EXPECTED_BEANS.entrySet()) {
            String type = entry.getKey();
            Class<? extends ActionInvokerBuilder> expectedClass = entry.getValue();

            assertThat(actionRuntime.getInvokerBuilder(type))
                    .as("ActionRuntime.getInvokerBuilder(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(actionRuntime.getInvokerBuilder(type))
                    .as("ActionRuntime.getInvokerBuilder(\"%s\") should be instance of %s", type, expectedClass.getSimpleName())
                    .isInstanceOf(expectedClass);
        }
    }

    @Test
    void testUnexpectedBeans() {
        for (ActionInvokerBuilder invokerBuilder : actionRuntime.getInvokerBuilders()) {
            assertThat(EXPECTED_BEANS)
                    .as("Found unexpected invoker builder bean for type \"%s\" of class %s. Add it to this test.", invokerBuilder.getType(), invokerBuilder.getClass())
                    .containsKey(invokerBuilder.getType());
        }
    }
}
