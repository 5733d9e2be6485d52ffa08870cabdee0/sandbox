package com.redhat.service.bridge.executor.actions;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.service.bridge.executor.actions.kafka.KafkaTopicActionInvokerFactory;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ActionInvokersTest {

    @Inject
    ActionInvokers actionInvokers;

    @InjectMock
    KafkaTopicActionInvokerFactory factory;

    @Test
    public void build() {

        ActionInvoker a = Mockito.mock(ActionInvoker.class);

        ProcessorDTO p = new ProcessorDTO();
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.KAFKA_ACTION_TYPE);
        p.setAction(b);

        when(factory.accepts(b)).thenReturn(true);
        when(factory.build(p, b)).thenReturn(a);

        ActionInvoker invoker = actionInvokers.build(p);
        assertThat(invoker, equalTo(a));
    }

    @Test
    public void build_noMatchingActionInvokerFactory() {
        ProcessorDTO p = new ProcessorDTO();
        BaseAction b = new BaseAction();
        b.setType("notRecognised");
        p.setAction(b);

        when(factory.accepts(b)).thenReturn(false);

        assertThrows(ActionInvokerException.class, () -> actionInvokers.build(p));
    }
}
