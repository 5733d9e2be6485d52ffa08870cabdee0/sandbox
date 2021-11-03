package com.redhat.service.bridge.shard.operator.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LabelsBuilderTest {

    @Test
    public void build() {
        assertThat(new LabelsBuilder().build().size()).isEqualTo(0);
        assertThat(new LabelsBuilder().withApplicationType("application").build().size()).isEqualTo(1);
    }

    @Test
    public void buildWithDefaults() {
        assertThat(new LabelsBuilder().buildWithDefaults().size()).isEqualTo(2);
        assertThat(new LabelsBuilder().withApplicationType("application").buildWithDefaults().size()).isEqualTo(3);
        assertThat(new LabelsBuilder().withCreatedBy("me").buildWithDefaults().size()).isEqualTo(2);
    }
}
