package com.redhat.service.smartevents.shard.operator.v2.resources;

public class KNativeBrokerConfigurationSpec {

    KafkaConfigurationSpec kafkaConfiguration;

    public KafkaConfigurationSpec getKafkaConfiguration() {
        return kafkaConfiguration;
    }

    public void setKafkaConfiguration(KafkaConfigurationSpec kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }
}
