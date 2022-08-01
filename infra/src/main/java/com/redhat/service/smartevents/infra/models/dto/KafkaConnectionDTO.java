package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaConnectionDTO {

    @JsonProperty("bootstrapServers")
    private String bootstrapServers;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("clientSecret")
    private String clientSecret;

    @JsonProperty("securityProtocol")
    private String securityProtocol;

    @JsonProperty("saslMechanism")
    private String saslMechanism;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("errorTopic")
    private String errorTopic;

    public KafkaConnectionDTO() {
    }

    public KafkaConnectionDTO(String bootstrapServers, String clientId, String clientSecret, String securityProtocol, String saslMechanism, String topic, String errorTopic) {
        this.bootstrapServers = bootstrapServers;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.securityProtocol = securityProtocol;
        this.saslMechanism = saslMechanism;
        this.topic = topic;
        this.errorTopic = errorTopic;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTopic() {
        return topic;
    }

    public String getErrorTopic() {
        return errorTopic;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setErrorTopic(String errorTopic) {
        this.errorTopic = errorTopic;
    }

    @Override
    public String toString() {
        return "KafkaConnection{" +
                "bootstrapServers='" + bootstrapServers + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret=<REDACTED>" +
                ", securityProtocol='" + securityProtocol + '\'' +
                ", saslMechanism='" + saslMechanism + '\'' +
                ", topic=" + topic + '\'' +
                ", errorTopic=" + errorTopic + '\'' +
                '}';
    }
}
