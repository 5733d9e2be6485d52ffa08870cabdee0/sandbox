package com.redhat.service.bridge.manager.api.user.validators.actions;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicActionValidator;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ActionParamValidatorContainerTest {

    @Inject
    ActionParamValidatorContainer container;

    ConstraintValidatorContext context;

    ConstraintValidatorContext.ConstraintViolationBuilder builder;

    private ProcessorRequest buildRequest() {
        ProcessorRequest p = new ProcessorRequest();
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        b.setParameters(params);
        p.setAction(b);
        return p;
    }

    @BeforeEach
    public void beforeEach() {
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builder);
    }

    @Test
    public void isValid() {
        ProcessorRequest p = buildRequest();
        assertThat(container.isValid(p, context)).isTrue();
    }

    @Test
    public void isValid_nullActionIsNotValid() {
        ProcessorRequest p = buildRequest();
        p.setAction(null);

        assertThat(container.isValid(p, context)).isFalse();
    }

    @Test
    public void isValid_emptyParamsIsNotValid() {
        ProcessorRequest p = buildRequest();
        p.getAction().getParameters().clear();

        assertThat(container.isValid(p, context)).isFalse();
    }

    @Test
    public void isValid_unrecognisedActionTypeIsNotValid() {
        ProcessorRequest p = buildRequest();
        p.getAction().setType("doesNotExist");

        assertThat(container.isValid(p, context)).isFalse();

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
        verify(builder).addConstraintViolation();
    }

    @Test
    public void isValid_messageFromActionValidatorAddedOnFailure() {
        ProcessorRequest p = buildRequest();
        p.getAction().getParameters().put(KafkaTopicAction.TOPIC_PARAM, "");

        assertThat(container.isValid(p, context)).isFalse();
        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(builder).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }
}
