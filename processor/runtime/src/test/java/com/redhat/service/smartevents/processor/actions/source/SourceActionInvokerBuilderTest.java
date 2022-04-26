package com.redhat.service.smartevents.processor.actions.source;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.actions.source.SourceAction.CLOUD_EVENT_TYPE_PARAM;
import static com.redhat.service.smartevents.processor.actions.source.SourceAction.ENDPOINT_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class SourceActionInvokerBuilderTest {

    public static final String TEST_ENDPOINT = "http://www.example.com/webhook";
    public static final String TEST_CLOUD_EVENT_TYPE = "TestSource";

    @Inject
    SourceActionInvokerBuilder builder;

    @Test
    void testInvokerOk() {
        ProcessorDTO processor = getProcessor(Map.of(ENDPOINT_PARAM, TEST_ENDPOINT, CLOUD_EVENT_TYPE_PARAM, TEST_CLOUD_EVENT_TYPE));
        Action action = processor.getDefinition().getResolvedAction();
        ActionInvoker actionInvoker = builder.build(processor, action);
        assertThat(actionInvoker)
                .isNotNull()
                .isInstanceOf(SourceActionInvoker.class);
    }

    @ParameterizedTest
    @MethodSource("invalidProcessors")
    void testInvokerException(ProcessorDTO processor) {
        Action action = processor.getDefinition().getResolvedAction();
        assertThatExceptionOfType(GatewayProviderException.class)
                .isThrownBy(() -> builder.build(processor, action));
    }

    private static Stream<Arguments> invalidProcessors() {
        return Stream.of(
                getProcessor(null),
                getProcessor(Collections.emptyMap()),
                getProcessor(Collections.singletonMap(ENDPOINT_PARAM, TEST_ENDPOINT)),
                getProcessor(Collections.singletonMap(CLOUD_EVENT_TYPE_PARAM, TEST_CLOUD_EVENT_TYPE))).map(Arguments::of);
    }

    private static ProcessorDTO getProcessor(Map<String, String> params) {
        Action action = new Action();
        action.setType(SourceAction.TYPE);
        action.setParameters(params);

        ProcessorDTO processor = new ProcessorDTO();
        processor.setType(ProcessorType.SINK);
        processor.setId("myProcessor");
        processor.setDefinition(new ProcessorDefinition(null, null, action));
        processor.setBridgeId("myBridge");

        return processor;
    }

}
