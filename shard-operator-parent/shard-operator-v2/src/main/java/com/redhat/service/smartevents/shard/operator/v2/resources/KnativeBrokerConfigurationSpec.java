package com.redhat.service.smartevents.shard.operator.v2.resources;

public class KnativeBrokerConfigurationSpec {

    KafkaConfigurationSpec kafkaConfiguration;

    public KnativeBrokerConfigurationSpec() {

    }

    public KnativeBrokerConfigurationSpec(KafkaConfigurationSpec kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }

    public KafkaConfigurationSpec getKafkaConfiguration() {
        return kafkaConfiguration;
    }

    public void setKafkaConfiguration(KafkaConfigurationSpec kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }
}
