package com.redhat.service.bridge.processor.actions.sendtobridge;

import com.redhat.service.bridge.processor.actions.common.ActionParameterValidatorFactory;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SendToBridgeActionTest {

    @Inject
    SendToBridgeAction sendToBridgeAction;

    @Inject
    ActionParameterValidatorFactory actionParameterValidatorFactory;

    @Test
    void testType() {
        assertThat(sendToBridgeAction.getType()).isEqualTo(SendToBridgeAction.TYPE);
    }

    @Test
    void testValidator() {
        assertThat(actionParameterValidatorFactory.get(SendToBridgeAction.TYPE)).isNotNull();
    }
}
