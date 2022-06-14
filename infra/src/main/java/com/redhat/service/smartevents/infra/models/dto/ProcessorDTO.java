package com.redhat.service.smartevents.infra.models.dto;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.processor.actions.KafkaTopicConstants;

public class ProcessorDTO {

    @JsonProperty("type")
    private ProcessorType type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("definition")
    private ProcessorDefinition definition;

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProcessorDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ProcessorDefinition definition) {
        this.definition = definition;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public KafkaConnectionDTO getKafkaConnection() {
        return kafkaConnection;
    }

    public void setKafkaConnection(KafkaConnectionDTO kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
    }

    public Optional<KafkaConnectionDTO> parseKafkaOutgoingConnection() {
        Action action = definition.getResolvedAction();
        String brokerUrl = action.getParameter(KafkaTopicConstants.KAFKA_BROKER_URL);
        String clientId = action.getParameter(KafkaTopicConstants.KAFKA_CLIENT_ID);
        String clientSecret = action.getParameter(KafkaTopicConstants.KAFKA_CLIENT_SECRET);
        String topic = action.getParameter(KafkaTopicConstants.TOPIC_PARAM);
        String securityProtocol = action.getParameter(KafkaTopicConstants.KAFKA_SECURITY_PROTOCOL);

        if (brokerUrl != null
                && clientId != null
                && clientSecret != null
                && topic != null
                && securityProtocol != null) {

            return Optional.of(new KafkaConnectionDTO(brokerUrl, clientId, clientSecret, securityProtocol, topic, kafkaConnection.getErrorTopic()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDTO that = (ProcessorDTO) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProcessorDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", definition=" + definition +
                ", bridgeId='" + bridgeId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                ", status=" + status +
                ", kafkaConnection=" + kafkaConnection +
                '}';
    }
}
