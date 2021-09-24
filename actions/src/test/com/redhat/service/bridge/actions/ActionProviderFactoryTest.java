package com.redhat.service.bridge.actions;

import javax.inject.Inject;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ActionProviderFactoryTest {

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Test
    public void getActionProvider() {
        Assertions.assertNotNull(actionProviderFactory.getActionProvider(KafkaTopicAction.TYPE));
    }

    @Test
    public void getActionProvider_actionTypeNotRecognised() {
        Assertions.assertThrows(ActionProviderException.class, () -> actionProviderFactory.getActionProvider("doesNotExist"));
    }
}
