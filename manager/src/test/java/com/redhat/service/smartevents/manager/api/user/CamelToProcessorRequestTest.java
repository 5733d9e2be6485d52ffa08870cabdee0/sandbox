package com.redhat.service.smartevents.manager.api.user;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.manager.api.models.requests.CamelProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CamelToProcessorRequestTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void conversionTest() throws IOException {

        String inputJson = "{\n" +
                "    \"name\": \"myProcessor\",\n" +
                "    \"flow\": [\n" +
                "        {\n" +
                "            \"from\": {\n" +
                "                \"uri\": \"rhose\",\n" +
                "                \"steps\": [\n" +
                "                    {\n" +
                "                        \"to\": {\n" +
                "                            \"uri\": \"rhoc:slack_sink_0.1:mySlack\",\n" +
                "                            \"parameters\": {\n" +
                "                                \"slack_webhook_url\": \"slack.com/url\",\n" +
                "                                \"slack_channel\": \"mychannel\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        String expectedJson = "{\n" +
                "  \"name\" : \"myProcessor\",\n" +
                "  \"filters\" : null,\n" +
                "  \"transformationTemplate\" : null,\n" +
                "  \"action\" : null,\n" +
                "  \"actions\" : [ {\n" +
                "    \"name\" : \"mySlack\",\n" +
                "    \"type\" : \"slack_sink_0.1\",\n" +
                "    \"parameters\" : {\n" +
                "      \"slack_channel\" : \"mychannel\",\n" +
                "      \"slack_webhook_url\" : \"slack.com/url\"\n" +
                "    }\n" +
                "  } ],\n" +
                "  \"source\" : null,\n" +
                "  \"processing\" : {\n" +
                "    \"type\" : \"cameldsl_0.1\",\n" +
                "    \"spec\" : {" +
                "\"flow\" : {\n" +
                "        \"from\" : {\n" +
                "          \"uri\" : \"rhose\",\n" +
                "          \"steps\" : [ {\n" +
                "            \"to\" : {\n" +
                "              \"uri\" : \"mySlack\"\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      }\n" +
                "}\n" +
                " }\n" +
                "}";

        CamelProcessorRequest camelRequest = objectMapper.readValue(inputJson, CamelProcessorRequest.class);

        ProcessorRequest actual = new CamelToProcessorRequest(camelRequest).convert();

        String actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual);

        ObjectNode prettyPrintedExpected = objectMapper.readValue(expectedJson, ObjectNode.class);
        String prettyPrintedExpectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyPrintedExpected);

        assertThat(actualJson).isEqualTo(prettyPrintedExpectedJson);
    }
}
