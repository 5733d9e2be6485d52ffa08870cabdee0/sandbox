package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;

public class KafkaConfigurationSpec {

    String bootstrapServers;

    String securityProtocol;

    String saslMechanism;

    String user;

    String password;

    String topic;

    int numberOfPartitions = 1;

    int numberOfReplicas = 3;

    public KafkaConfigurationSpec() {

    }

    private KafkaConfigurationSpec(Builder builder) {
        setBootstrapServers(builder.bootstrapServers);
        setSecurityProtocol(builder.securityProtocol);
        setSaslMechanism(builder.saslMechanism);
        setUser(builder.user);
        setPassword(builder.password);
        setTopic(builder.topic);
        setNumberOfPartitions(builder.numberOfPartitions);
        setNumberOfReplicas(builder.numberOfReplicas);
    }

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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
                && Objects.equals(securityProtocol, that.securityProtocol) && Objects.equals(saslMechanism, that.saslMechanism) && Objects.equals(user, that.user)
                && Objects.equals(password, that.password) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bootstrapServers, securityProtocol, saslMechanism, user, password, topic, numberOfPartitions, numberOfReplicas);
    }

    public static final class Builder {

        private String bootstrapServers;
        private String securityProtocol;
        private String saslMechanism;
        private String user;
        private String password;
        private String topic;
        private int numberOfPartitions;
        private int numberOfReplicas;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder bootstrapServers(String val) {
            bootstrapServers = val;
            return this;
        }

        public Builder securityProtocol(String val) {
            securityProtocol = val;
            return this;
        }

        public Builder saslMechanism(String val) {
            saslMechanism = val;
            return this;
        }

        public Builder user(String val) {
            user = val;
            return this;
        }

        public Builder password(String val) {
            password = val;
            return this;
        }

        public Builder topic(String val) {
            topic = val;
            return this;
        }

        public Builder numberOfPartitions(int val) {
            numberOfPartitions = val;
            return this;
        }

        public Builder numberOfReplicas(int val) {
            numberOfReplicas = val;
            return this;
        }

        public KafkaConfigurationSpec build() {
            return new KafkaConfigurationSpec(this);
        }
    }
}
