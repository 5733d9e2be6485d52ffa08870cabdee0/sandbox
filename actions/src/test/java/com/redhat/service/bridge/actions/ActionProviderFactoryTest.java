package com.redhat.service.bridge.actions;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
public class ActionProviderFactoryTest {

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Test
    public void getActionProvider() {
        assertThat(actionProviderFactory.getActionProvider(KafkaTopicAction.TYPE)).isNotNull();
    }

    @Test
    public void getActionProvider_actionTypeNotRecognised() {
        assertThatExceptionOfType(ActionProviderException.class).isThrownBy(() -> actionProviderFactory.getActionProvider("doesNotExist"));
    }
}
