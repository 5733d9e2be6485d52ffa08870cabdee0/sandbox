package com.redhat.service.bridge.processor.actions.sendtobridge;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.processor.actions.ActionParameterValidatorFactory;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SendToBridgeActionTest {

    @Inject
    SendToBridgeActionBean sendToBridgeAction;

    @Inject
    ActionParameterValidatorFactory actionParameterValidatorFactory;

    @Test
    void testType() {
        assertThat(sendToBridgeAction.getType()).isEqualTo(SendToBridgeActionBean.TYPE);
    }

    @Test
    void testValidator() {
        assertThat(actionParameterValidatorFactory.get(SendToBridgeActionBean.TYPE)).isNotNull();
    }
}
