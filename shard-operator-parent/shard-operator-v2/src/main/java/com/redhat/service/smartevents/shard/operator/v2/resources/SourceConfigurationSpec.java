package com.redhat.service.smartevents.shard.operator.v2.resources;

public class SourceConfigurationSpec {

    KafkaConfigurationSpec kafkaConfiguration;

    public SourceConfigurationSpec() {

    }

    public SourceConfigurationSpec(KafkaConfigurationSpec kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }

    public KafkaConfigurationSpec getKafkaConfiguration() {
        return kafkaConfiguration;
    }

    public void setKafkaConfiguration(KafkaConfigurationSpec kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }
}
