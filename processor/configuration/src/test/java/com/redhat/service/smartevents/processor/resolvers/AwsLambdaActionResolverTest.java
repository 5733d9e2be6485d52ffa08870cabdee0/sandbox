package com.redhat.service.smartevents.processor.resolvers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.resolvers.custom.AwsLambdaActionResolver;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class AwsLambdaActionResolverTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_TOPIC_NAME = "ob-test-processor-id";

    private static final String AWS_FUNCTION_PARAM = "aws_function";
    private static final String AWS_REGION_PARAM = "aws_region";
    private static final String AWS_ACCESS_KEY_ID_PARAM = "aws_access_key";
    private static final String AWS_SECRET_ACCESS_KEY_PARAM = "aws_secret_key";

    @Inject
    AwsLambdaActionResolver awsLambdaActionResolver;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getConnectorTopicName(TEST_PROCESSOR_ID)).thenReturn(TEST_PROCESSOR_TOPIC_NAME);
        when(gatewayConfiguratorServiceMock.getConnectorTopicName(not(eq(TEST_PROCESSOR_ID)))).thenThrow(new IllegalStateException());
    }

    @Test
    void testTransform() {
        Action action = buildTestAction();

        Action transformedAction = awsLambdaActionResolver.resolve(action, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        assertThat(transformedAction.getParameter(AWS_FUNCTION_PARAM)).isEqualTo("function");
        assertThat(transformedAction.getParameter(AWS_REGION_PARAM)).isEqualTo("eu-north-1");
        assertThat(transformedAction.getParameter(AWS_ACCESS_KEY_ID_PARAM)).isEqualTo("keyid");
        assertThat(transformedAction.getParameter(AWS_SECRET_ACCESS_KEY_PARAM)).isEqualTo("key");

        assertThat(transformedAction.getParameter(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(TEST_PROCESSOR_TOPIC_NAME);
    }

    private Action buildTestAction() {
        Map<String, String> parameters = Map.of(
                AWS_FUNCTION_PARAM, "function",
                AWS_REGION_PARAM, "eu-north-1",
                AWS_ACCESS_KEY_ID_PARAM, "keyid",
                AWS_SECRET_ACCESS_KEY_PARAM, "key");

        Action action = new Action();
        action.setType(AwsLambdaAction.TYPE);
        action.setMapParameters(parameters);
        return action;
    }
}
