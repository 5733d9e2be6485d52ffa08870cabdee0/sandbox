package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;

import io.fabric8.kubernetes.api.model.Secret;

import static org.assertj.core.api.Assertions.assertThat;

class CamelIntegrationTest {

    @Test
    public void convertComplexCamelSpec() throws Exception {
        String spec = "{ \"flow\": {\n" +
                "        \"from\": {\n" +
                "          \"uri\": \"rhose\",\n" +
                "          \"steps\": [\n" +
                "            {\n" +
                "              \"unmarshal\": {\n" +
                "                \"json\": {}\n" +
                "              }\n" +
                "            }," +
                "            {\n" +
                "              \"choice\": {\n" +
                "                \"when\": [\n" +
                "                  {\n" +
                "                    \"simple\": \"${body[nutritions][sugar]} <= 5\",\n" +
                "                    \"steps\": [\n" +
                "                      {\n" +
                "                        \"log\": { \"message\" : \"++++- Lesser equal than 5 ${body}\" }\n" +
                "                      },\n" +
                "                      {\n" +
                "                        \"marshal\": {\n" +
                "                          \"json\": {}\n" +
                "                        }\n" +
                "                      }," +
                "                      {\n" +
                "                        \"to\": { \"uri\" : \"mySlackAction\" } \n" +
                "                      }\n" +
                "                    ]\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"simple\": \"${body[nutritions][sugar]} > 5 && ${body[nutritions][sugar]} <= 10\",\n" +
                "                    \"steps\": [\n" +
                "                      {\n" +
                "                        \"log\": { \"message\" : \"++++- between 5 and 10 goes to mc ${body}\" }\n" +
                "                      },\n" +
                "                      {\n" +
                "                        \"marshal\": {\n" +
                "                          \"json\": {}\n" +
                "                        }\n" +
                "                      }," +
                "                      {\n" +
                "                        \"to\": { \"uri\" : \"myServiceNowAction\" } \n" +
                "                      }\n" +
                "                    ]\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"otherwise\": {\n" +
                "                  \"steps\": [\n" +
                "                      {\n" +
                "                        \"marshal\": {\n" +
                "                          \"json\": {}\n" +
                "                        }\n" +
                "                      }," +
                "                    {\n" +
                "                      \"to\": { \"uri\" : \"errorAction\" }\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      } }";

        String expectedYaml =
                "apiVersion: camel.apache.org/v1\n" +
                        "kind: Integration\n" +
                        "metadata:\n" +
                        "  labels:\n" +
                        "    app.kubernetes.io/managed-by: bridge-fleet-shard-operator\n" +
                        "    app.kubernetes.io/created-by: bridge-fleet-shard-operator\n" +
                        "  name: ob-camel-processorid\n" +
                        "  namespace: namespace\n" +
                        "spec:\n" +
                        "  flows:\n" +
                        "  - from:\n" +
                        "      steps:\n" +
                        "      - unmarshal:\n" +
                        "          json: {}\n" +
                        "      - choice:\n" +
                        "          when:\n" +
                        "          - simple: ${body[nutritions][sugar]} <= 5\n" +
                        "            steps:\n" +
                        "            - log:\n" +
                        "                message: ++++- Lesser equal than 5 ${body}\n" +
                        "            - marshal:\n" +
                        "                json: {}\n" +
                        "            - to:\n" +
                        "                uri: kafka:topicSlackAction\n" +
                        "                parameters:\n" +
                        "                  brokers: bootstrapServer\n" +
                        "                  securityProtocol: SASL_SSL\n" +
                        "                  groupId: kafkaGroup\n" +
                        "                  maxPollRecords: 5000\n" +
                        "                  saslMechanism: PLAIN\n" +
                        "                  saslJaasConfig: org.apache.kafka.common.security.plain.PlainLoginModule\n" +
                        "                    required username='clientId'\n" +
                        "                    password='clientSecret';\n" +
                        "                  consumersCount: 1\n" +
                        "                  seekTo: beginning\n" +
                        "          - simple: ${body[nutritions][sugar]} > 5 && ${body[nutritions][sugar]} <= 10\n" +
                        "            steps:\n" +
                        "            - log:\n" +
                        "                message: ++++- between 5 and 10 goes to mc ${body}\n" +
                        "            - marshal:\n" +
                        "                json: {}\n" +
                        "            - to:\n" +
                        "                uri: kafka:topicServiceNowAction\n" +
                        "                parameters:\n" +
                        "                  brokers: bootstrapServer\n" +
                        "                  securityProtocol: SASL_SSL\n" +
                        "                  groupId: kafkaGroup\n" +
                        "                  maxPollRecords: 5000\n" +
                        "                  saslMechanism: PLAIN\n" +
                        "                  saslJaasConfig: org.apache.kafka.common.security.plain.PlainLoginModule\n" +
                        "                    required username='clientId'\n" +
                        "                    password='clientSecret';\n" +
                        "                  consumersCount: 1\n" +
                        "                  seekTo: beginning\n" +
                        "          otherwise:\n" +
                        "            steps:\n" +
                        "            - marshal:\n" +
                        "                json: {}\n" +
                        "            - to:\n" +
                        "                uri: kafka:topicErrorAction\n" +
                        "                parameters:\n" +
                        "                  brokers: bootstrapServer\n" +
                        "                  securityProtocol: SASL_SSL\n" +
                        "                  groupId: kafkaGroup\n" +
                        "                  maxPollRecords: 5000\n" +
                        "                  saslMechanism: PLAIN\n" +
                        "                  saslJaasConfig: org.apache.kafka.common.security.plain.PlainLoginModule\n" +
                        "                    required username='clientId'\n" +
                        "                    password='clientSecret';\n" +
                        "                  consumersCount: 1\n" +
                        "                  seekTo: beginning\n" +
                        "      uri: kafka:bridgeTopic\n" +
                        "      parameters:\n" +
                        "        brokers: bootstrapServer\n" +
                        "        securityProtocol: SASL_SSL\n" +
                        "        groupId: kafkaGroup\n" +
                        "        maxPollRecords: 5000\n" +
                        "        saslMechanism: PLAIN\n" +
                        "        saslJaasConfig: org.apache.kafka.common.security.plain.PlainLoginModule required\n" +
                        "          username='clientId' password='clientSecret';\n" +
                        "        consumersCount: 1\n" +
                        "        seekTo: beginning\n";

        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setId("processorId");
        ProcessorDefinition definition = new ProcessorDefinition();

        ArrayList<Action> resolvedActions = new ArrayList<>();
        resolvedActions.add(action("mySlackAction", "topicSlackAction"));
        resolvedActions.add(action("myServiceNowAction", "topicServiceNowAction"));
        resolvedActions.add(action("errorAction", "topicErrorAction"));
        definition.setResolvedActions(resolvedActions);
        Processing processing = new Processing();

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonSpec = mapper.readValue(spec, ObjectNode.class);
        processing.setSpec(jsonSpec);

        definition.setProcessing(processing);
        processorDTO.setDefinition(definition);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        Secret secret = new Secret();
        HashMap<String, String> secretData = new HashMap<>();
        secretData.put(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR, toBase64("bridgeTopic"));
        secretData.put(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR, toBase64("bootstrapServer"));
        secretData.put(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR, toBase64("clientId"));
        secretData.put(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR, toBase64("clientSecret"));

        secret.setData(secretData);

        CamelIntegration actual = CamelIntegration.fromDTO(processorDTO, "namespace", processing, secret);
        String actualAsYamlString = yamlMapper.writeValueAsString(actual);

        ObjectNode expectedParsed = yamlMapper.readValue(expectedYaml, ObjectNode.class);
        ObjectNode actualParsed = yamlMapper.readValue(actualAsYamlString, ObjectNode.class);

        assertJsonEquals(expectedParsed, actualParsed);
    }

    private void assertJsonEquals(ObjectNode expected, ObjectNode actual) {
        assertThat(actual.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @NotNull
    private Action action(String name, String kafkaTopic) {
        Action action = new Action();
        action.setName(name);
        action.setType(KafkaTopicAction.TYPE);
        action.setMapParameters(Map.of("topic", kafkaTopic));
        return action;
    }

    Base64.Encoder encoder = Base64.getEncoder();

    private String toBase64(String inputString) {
        return encoder.encodeToString(inputString.getBytes(StandardCharsets.UTF_8));
    }
}
