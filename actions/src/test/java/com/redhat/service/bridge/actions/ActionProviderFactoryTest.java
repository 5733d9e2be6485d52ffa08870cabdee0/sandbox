package com.redhat.service.bridge.actions;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class ActionProviderFactoryTest {

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Test
    void getKafkaTopicActionProvider() {
        assertThat(actionProviderFactory.getActionProvider(KafkaTopicAction.TYPE))
                .isNotNull()
                .isInstanceOf(KafkaTopicAction.class);
    }

    @Test
    void getWebhookActionProvider() {
        assertThat(actionProviderFactory.getActionProvider(WebhookAction.TYPE))
                .isNotNull()
                .isInstanceOf(WebhookAction.class);
    }

    @Test
    void getActionProvider_actionTypeNotRecognised() {
        assertThatExceptionOfType(ActionProviderException.class)
                .isThrownBy(() -> actionProviderFactory.getActionProvider("doesNotExist"));
    }
}
