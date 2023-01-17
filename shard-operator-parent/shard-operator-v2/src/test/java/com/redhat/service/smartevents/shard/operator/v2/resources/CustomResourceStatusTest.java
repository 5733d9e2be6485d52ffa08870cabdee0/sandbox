package com.redhat.service.smartevents.shard.operator.v2.resources;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomResourceStatusTest {

    @Test
    public void testMarkConditionReady() {
        // Given
        final BaseResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(DefaultConditions.CP_CONTROL_PLANE_READY_NAME);
        resourceStatus.markConditionTrue(DefaultConditions.CP_DATA_PLANE_READY_NAME);

        // Then
        assertThat(resourceStatus.isReady()).isTrue();
    }

    @Test
    public void testMarkConditionFalse() {
        // Given
        final BaseResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, "Done");
        resourceStatus.markConditionFalse(DefaultConditions.CP_DATA_PLANE_READY_NAME);

        // Then
        assertThat(resourceStatus.isReady()).isFalse();
        assertThat(resourceStatus.getConditionByType(DefaultConditions.CP_CONTROL_PLANE_READY_NAME))
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.getLastTransitionTime()).isNotNull();
                    assertThat(c.getReason()).isEqualTo("Done");
                });
    }

    @Test
    public void testMarkConditionFailure() {
        // Given
        final BaseResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, "Done");
        resourceStatus.markConditionFailed(DefaultConditions.CP_DATA_PLANE_READY_NAME);

        // Then
        assertThat(resourceStatus.isReady()).isFalse();
        assertThat(resourceStatus.getConditionByType(DefaultConditions.CP_CONTROL_PLANE_READY_NAME))
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.getLastTransitionTime()).isNotNull();
                    assertThat(c.getReason()).isEqualTo("Done");
                });
    }

}
