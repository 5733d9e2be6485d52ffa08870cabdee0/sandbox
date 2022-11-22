package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;

public class KafkaConfigurationSpec {

    String bootstrapServers;

    String securityProtocol;

    String saslMechanism;

    String userId;

    String password;

    String topicName;

    int numberOfPartitions;

    int numberOfReplicas;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getNumberOfPartitions() {
        return numberOfPartitions;
    }

    public void setNumberOfPartitions(int numberOfPartitions) {
        this.numberOfPartitions = numberOfPartitions;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KafkaConfigurationSpec that = (KafkaConfigurationSpec) o;
        return numberOfPartitions == that.numberOfPartitions && numberOfReplicas == that.numberOfReplicas && Objects.equals(bootstrapServers, that.bootstrapServers)
                && Objects.equals(securityProtocol, that.securityProtocol) && Objects.equals(saslMechanism, that.saslMechanism) && Objects.equals(userId, that.userId)
                && Objects.equals(password, that.password) && Objects.equals(topicName, that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bootstrapServers, securityProtocol, saslMechanism, userId, password, topicName, numberOfPartitions, numberOfReplicas);
    }
}
