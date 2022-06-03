package com.redhat.service.smartevents.processor.sources.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.gateways.Source;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AwsS3SourceConnectorTest {

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_REGION = "test-region";
    private static final String TEST_ACCESS_KEY = "test-access-key";
    private static final String TEST_SECRET_KEY = "test-secret-key";
    private static final Boolean TEST_IGNORE_BODY = true;
    private static final Boolean TEST_DELETE_AFTER_READ = false;
    private static final String TEST_TOPIC_NAME = "test-topic-name";
    private static final String ERROR_HANDLER_TOPIC_NAME = "errorHandlerTopic";
    private static final String TEST_PREFIX = "test-prefix";

    private static final String EXPECTED_PAYLOAD_JSON = "{" +
            "  \"aws_bucket_name_or_arn\":\"" + TEST_BUCKET + "\"," +
            "  \"aws_region\":\"" + TEST_REGION + "\"," +
            "  \"aws_access_key\":\"" + TEST_ACCESS_KEY + "\"," +
            "  \"aws_secret_key\":\"" + TEST_SECRET_KEY + "\"," +
            "  \"aws_prefix\":\"" + TEST_PREFIX + "\"," +
            "  \"aws_ignore_body\":" + TEST_IGNORE_BODY + "," +
            "  \"aws_delete_after_read\":" + TEST_DELETE_AFTER_READ + "," +
            "  \"kafka_topic\":\"" + TEST_TOPIC_NAME + "\"," +
            "  \"processors\": [" +
            "    {" +
            "      \"log\": {" +
            "        \"multiLine\":true," +
            "        \"showHeaders\":true" +
            "      }" +
            "    }" +
            "  ], " +
            "  \"error_handler\": {" +
            "    \"dead_letter_queue\": {" +
            "      \"topic\": \"errorHandlerTopic\"" +
            "    }" +
            "  }" +
            "}";

    @Inject
    AwsS3SourceConnector connector;

    @Inject
    ObjectMapper mapper;

    @Test
    void testConnectorType() {
        assertThat(connector.getConnectorTypeId()).isEqualTo(AwsS3SourceConnector.CONNECTOR_TYPE_ID);
    }

    @Test
    void testConnectorPayload() throws JsonProcessingException {
        JsonNode expectedPayload = mapper.readTree(EXPECTED_PAYLOAD_JSON);

        Source source = new Source();
        source.setType(AwsS3SourceConnector.TYPE);
        source.setMapParameters(Map.of(AwsS3Source.BUCKET_NAME_OR_ARN_PARAMETER, TEST_BUCKET,
                AwsS3Source.REGION_PARAMETER, TEST_REGION,
                AwsS3Source.ACCESS_KEY_PARAMETER, TEST_ACCESS_KEY,
                AwsS3Source.SECRET_KEY_PARAMETER, TEST_SECRET_KEY,
                AwsS3Source.PREFIX, TEST_PREFIX,
                AwsS3Source.IGNORE_BODY_PARAMETER, TEST_IGNORE_BODY.toString(),
                AwsS3Source.DELETE_AFTER_READ_PARAMETER, TEST_DELETE_AFTER_READ.toString()));

        JsonNode payload = connector.connectorPayload(source, TEST_TOPIC_NAME, ERROR_HANDLER_TOPIC_NAME);

        assertThat(payload).isEqualTo(expectedPayload);
    }

    @Test
    void testEmptyPrefix() {
        Source source = new Source();
        source.setType(AwsS3SourceConnector.TYPE);
        source.setMapParameters(Map.of(AwsS3Source.PREFIX, ""));

        JsonNode payload = connector.connectorPayload(source, TEST_TOPIC_NAME, ERROR_HANDLER_TOPIC_NAME);

        assertThat(payload.asText()).doesNotContain("aws_prefix");
    }

    @Test
    void testNullPrefix() {
        Source source = new Source();
        source.setType(AwsS3SourceConnector.TYPE);
        Map<String, String> values = new HashMap<>();
        values.put(AwsS3Source.PREFIX, null);
        source.setMapParameters(values);

        JsonNode payload = connector.connectorPayload(source, TEST_TOPIC_NAME, ERROR_HANDLER_TOPIC_NAME);

        assertThat(payload.asText()).doesNotContain("aws_prefix");
    }
}
