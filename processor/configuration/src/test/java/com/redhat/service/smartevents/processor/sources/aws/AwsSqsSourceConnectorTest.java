package com.redhat.service.smartevents.processor.sources.aws;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayValidator;
import com.redhat.service.smartevents.processor.sources.AbstractSourceTest;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_ACCESS_KEY_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_OVERRIDE_ENDPOINT_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_QUEUE_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_QUEUE_URL_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_REGION_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_SECRET_KEY_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_AWS_URI_ENDPOINT_OVERRIDE_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceConnector.CONNECTOR_TOPIC_PARAMETER;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_AWS_ACCESS_KEY_ID;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_AWS_QUEUE_URL;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_AWS_REGION;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_AWS_SECRET_ACCESS_KEY;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_GENERIC_ENDPOINT;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_GENERIC_QUEUE_URL;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.VALID_QUEUE_NAME;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidatorTest.paramMap;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AwsSqsSourceConnectorTest extends AbstractSourceTest<Source> {

    private static final String CHANNEL = "channel";
    private static final String TOKEN = "token";
    private static final String TOPIC_NAME = "topic";

    private static final String PARAMETER_TEMPLATE = "\"%s\":\"%s\",";
    private static final String PARAMETER_TEMPLATE_NO_QUOTES = "\"%s\":%s,";
    private static final String EXPECTED_PAYLOAD_JSON_TEMPLATE = "{" +
            "   \"" + CONNECTOR_TOPIC_PARAMETER + "\":\"" + TOPIC_NAME + "\"," +
            "   %s" +
            "   \"processors\": [" +
            "       {" +
            "           \"log\": {" +
            "               \"multiLine\":true," +
            "               \"showHeaders\":true" +
            "        }" +
            "     }" +
            "   ]" +
            "}";

    static final Object[][] VALID_PARAMS = {
            {
                    paramMap(VALID_AWS_QUEUE_URL, null, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
                    generateExpectedPayload(VALID_AWS_QUEUE_URL, VALID_QUEUE_NAME, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY, VALID_AWS_REGION, null, null)
            },
            {
                    paramMap(VALID_AWS_QUEUE_URL, "eu-west-1", VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
                    generateExpectedPayload(VALID_AWS_QUEUE_URL, VALID_QUEUE_NAME, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY, VALID_AWS_REGION, null, null)
            },
            {
                    paramMap(VALID_GENERIC_QUEUE_URL, null, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
                    generateExpectedPayload(VALID_GENERIC_QUEUE_URL, VALID_QUEUE_NAME, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY, VALID_AWS_REGION, "true", VALID_GENERIC_ENDPOINT)
            },
            {
                    paramMap(VALID_GENERIC_QUEUE_URL, "eu-west-1", VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
                    generateExpectedPayload(VALID_GENERIC_QUEUE_URL, VALID_QUEUE_NAME, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY, "eu-west-1", "true", VALID_GENERIC_ENDPOINT)
            }
    };

    @Inject
    AwsSqsSourceConnector connector;

    @Inject
    ObjectMapper mapper;

    @Test
    void testConnectorType() {
        assertThat(connector.getConnectorTypeId()).isEqualTo(AwsSqsSourceConnector.CONNECTOR_TYPE_ID);
    }

    @ParameterizedTest
    @MethodSource("validParams")
    void testConnectorPayload(Map<String, String> params, String expectedPayload) throws JsonProcessingException {
        JsonNode expectedPayloadJsonNode = mapper.readTree(expectedPayload);
        JsonNode payload = connector.connectorPayload(sourceWith(params), TOPIC_NAME);
        assertThat(payload).isEqualTo(expectedPayloadJsonNode);
    }

    private static Stream<Arguments> validParams() {
        return Arrays.stream(VALID_PARAMS).map(Arguments::of);
    }

    private static String generateExpectedPayload(String queueUrl, String queueName, String accessKey, String secretKey, String region, String overrideEndpoint, String uriEndpoint) {
        StringBuffer payload = new StringBuffer();
        if (queueUrl != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_QUEUE_URL_PARAMETER, queueUrl));
        }
        if (queueName != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_QUEUE_PARAMETER, queueName));
        }
        if (accessKey != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_ACCESS_KEY_PARAMETER, accessKey));
        }
        if (secretKey != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_SECRET_KEY_PARAMETER, secretKey));
        }
        if (region != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_REGION_PARAMETER, region));
        }
        if (overrideEndpoint != null) {
            payload.append(String.format(PARAMETER_TEMPLATE_NO_QUOTES, CONNECTOR_AWS_OVERRIDE_ENDPOINT_PARAMETER, overrideEndpoint));
        }
        if (uriEndpoint != null) {
            payload.append(String.format(PARAMETER_TEMPLATE, CONNECTOR_AWS_URI_ENDPOINT_OVERRIDE_PARAMETER, uriEndpoint));
        }
        return String.format(EXPECTED_PAYLOAD_JSON_TEMPLATE, payload);
    }

    @Override
    protected GatewayValidator<Source> getValidator() {
        // Validator not tested in this test
        return null;
    }

    @Override
    protected String getSourceType() {
        return AwsSqsSource.TYPE;
    }
}
