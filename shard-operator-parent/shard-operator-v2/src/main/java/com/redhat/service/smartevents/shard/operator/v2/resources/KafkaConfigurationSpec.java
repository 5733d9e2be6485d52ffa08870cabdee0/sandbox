package com.redhat.service.smartevents.shard.operator.v2.resources;

public class KafkaConfigurationSpec {

    String bootstrapServers;

    String topic;

    public KafkaConfigurationSpec() {

    }

    private KafkaConfigurationSpec(Builder builder) {
        setBootstrapServers(builder.bootstrapServers);
        setTopic(builder.topic);
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public static final class Builder {

        private String bootstrapServers;
        private String topic;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder bootstrapServers(String val) {
            bootstrapServers = val;
            return this;
        }

        public Builder topic(String val) {
            topic = val;
            return this;
        }

        public KafkaConfigurationSpec build() {
            return new KafkaConfigurationSpec(this);
        }
    }
}
