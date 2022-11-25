package com.redhat.service.smartevents.shard.operator.core.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LabelsBuilderTest {

    @Test
    public void build() {
        assertThat(new LabelsBuilder().build().size()).isEqualTo(0);
        assertThat(new LabelsBuilder().withComponent("component").build().size()).isEqualTo(1);
    }

    @Test
    public void buildWithDefaults() {
        assertThat(new LabelsBuilder().buildWithDefaults(LabelsBuilder.V1_OPERATOR_NAME).size()).isEqualTo(2);
        assertThat(new LabelsBuilder().buildWithDefaults(LabelsBuilder.V2_OPERATOR_NAME)).containsEntry(LabelsBuilder.MANAGED_BY_LABEL, LabelsBuilder.V2_OPERATOR_NAME).containsEntry(LabelsBuilder.CREATED_BY_LABEL, LabelsBuilder.V2_OPERATOR_NAME);
        assertThat(new LabelsBuilder().withComponent("component").buildWithDefaults(LabelsBuilder.V1_OPERATOR_NAME).size()).isEqualTo(3);
        assertThat(new LabelsBuilder().withCreatedBy("me").buildWithDefaults(LabelsBuilder.V1_OPERATOR_NAME).size()).isEqualTo(2);
    }
}
