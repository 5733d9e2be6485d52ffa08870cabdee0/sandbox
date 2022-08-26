package com.redhat.service.smartevents.integration.tests.resources.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.keycloak.representations.AccessTokenResponse;

import com.openshift.cloud.api.kas.auth.AclsApi;
import com.openshift.cloud.api.kas.auth.TopicsApi;
import com.openshift.cloud.api.kas.auth.invoker.ApiClient;
import com.openshift.cloud.api.kas.auth.invoker.Configuration;
import com.openshift.cloud.api.kas.auth.invoker.auth.OAuth;
import com.openshift.cloud.api.kas.auth.models.AclBinding;
import com.openshift.cloud.api.kas.auth.models.AclOperation;
import com.openshift.cloud.api.kas.auth.models.AclOperationFilter;
import com.openshift.cloud.api.kas.auth.models.AclPatternType;
import com.openshift.cloud.api.kas.auth.models.AclPatternTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclPermissionType;
import com.openshift.cloud.api.kas.auth.models.AclPermissionTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclResourceType;
import com.openshift.cloud.api.kas.auth.models.AclResourceTypeFilter;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class KafkaResource {
    public static void createKafkaTopic(String topicName) {
        KafkaConnectionParameters kafkaConnectionParameters = KafkaConnectionParameters.getInstance();
        ApiClient apiClient = createApiClient();
        TopicsApi topicsApi = new TopicsApi(apiClient);
        NewTopicInput newTopicInput = new NewTopicInput()
                .name(topicName)
                .settings(new TopicSettings().numPartitions(1));
        try {
            Thread.sleep(5000);
            topicsApi.createTopic(newTopicInput);
        } catch (Exception e) {
            throw new RuntimeException("Error creating new topic", e);
        }

        AclsApi aclsApi = new AclsApi(apiClient);
        AclBinding aclBindingRead = new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal("User:" + kafkaConnectionParameters.getOpsClientID())
                .operation(AclOperation.READ)
                .permission(AclPermissionType.ALLOW);
        AclBinding aclBindingWrite = new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal("User:" + kafkaConnectionParameters.getOpsClientID())
                .operation(AclOperation.WRITE)
                .permission(AclPermissionType.ALLOW);
        try {
            aclsApi.createAcl(aclBindingRead);
        } catch (Exception e) {
            try {
                topicsApi.deleteTopic(topicName);
            } catch (Exception ex) {
                throw new RuntimeException("Error deleting topic", ex);
            }
            throw new RuntimeException("Error assigning READ ACL to topic", e);
        }
        try {
            aclsApi.createAcl(aclBindingWrite);
        } catch (Exception e) {
            try {
                topicsApi.deleteTopic(topicName);
                aclsApi.deleteAcls(AclResourceTypeFilter.TOPIC,
                        topicName,
                        AclPatternTypeFilter.LITERAL,
                        "User:" + kafkaConnectionParameters.getOpsClientID(),
                        AclOperationFilter.READ,
                        AclPermissionTypeFilter.ALLOW);
            } catch (Exception ex) {
                throw new RuntimeException("Error cleaning up topic after creating of ACL failed", ex);
            }
            throw new RuntimeException("Error assigning WRITE ACL to topic", e);
        }
    }

    public static void deleteKafkaTopic(String topicName) {
        KafkaConnectionParameters kafkaConnectionParameters = KafkaConnectionParameters.getInstance();
        ApiClient apiClient = createApiClient();
        AclsApi aclsApi = new AclsApi(apiClient);
        try {
            aclsApi.deleteAcls(AclResourceTypeFilter.TOPIC,
                    topicName,
                    AclPatternTypeFilter.LITERAL,
                    "User:" + kafkaConnectionParameters.getOpsClientID(),
                    AclOperationFilter.READ,
                    AclPermissionTypeFilter.ALLOW);
            aclsApi.deleteAcls(AclResourceTypeFilter.TOPIC,
                    topicName,
                    AclPatternTypeFilter.LITERAL,
                    "User:" + kafkaConnectionParameters.getOpsClientID(),
                    AclOperationFilter.WRITE,
                    AclPermissionTypeFilter.ALLOW);
        } catch (Exception e) {
            throw new RuntimeException("Error removing ACL for topic", e);
        }

        TopicsApi topicsApi = new TopicsApi(apiClient);
        try {
            topicsApi.deleteTopic(topicName);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting topic", e);
        }
    }

    public static List<String> readTopic(String topicName) {
        KafkaConnectionParameters kafkaConnectionParameters = KafkaConnectionParameters.getInstance();
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionParameters.getKafkaBootstrapServer());
        properties.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                        kafkaConnectionParameters.getOpsClientID() +
                        "\" password=\"" +
                        kafkaConnectionParameters.getOpsClientSecret() +
                        "\";");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singleton(topicName));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(15));

        return StreamSupport.stream(records.spliterator(), false).map(s -> s.value()).collect(Collectors.toList());
    }

    private static ApiClient createApiClient() {
        KafkaConnectionParameters kafkaConnectionParameters = KafkaConnectionParameters.getInstance();
        ApiClient client = Configuration.getDefaultApiClient();
        client.setBasePath(kafkaConnectionParameters.getManagedKafkaRestURL());
        final String token = kafkaBearerToken();
        OAuth bearer = (OAuth) client.getAuthentication("Bearer");
        bearer.setAccessToken(token);

        return client;
    }

    private static String kafkaBearerToken() {
        KafkaConnectionParameters kafkaConnectionParameters = KafkaConnectionParameters.getInstance();

        AccessTokenResponse accessTokenResponse = given().param("grant_type", "client_credentials")
                .param("client_id", kafkaConnectionParameters.getAdminClientID())
                .param("client_secret", kafkaConnectionParameters.getAdminClientSecret())
                .contentType("application/x-www-form-urlencoded")
                .accept(ContentType.JSON)
                .when()
                .post(kafkaConnectionParameters.getAuthServerURL() + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class);

        return accessTokenResponse.getToken();
    }
}
