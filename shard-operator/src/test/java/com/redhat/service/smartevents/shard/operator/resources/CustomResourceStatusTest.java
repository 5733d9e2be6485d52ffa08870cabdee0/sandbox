package com.redhat.service.smartevents.shard.operator.resources;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomResourceStatusTest {

    @Test
    public void testMarkConditionReady() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(ConditionTypeConstants.READY);
        resourceStatus.markConditionTrue(ConditionTypeConstants.AUGMENTATION);

        // Then
        assertThat(resourceStatus.isReady()).isTrue();
    }

    @Test
    public void testMarkConditionFailure() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_FAILED, "");
        resourceStatus.markConditionTrue(ConditionTypeConstants.AUGMENTATION);

        // Then
        assertThat(resourceStatus.isReady()).isFalse();
        assertThat(resourceStatus.getConditionByType(ConditionTypeConstants.READY)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getReason()).isEqualTo(ConditionReasonConstants.DEPLOYMENT_FAILED);
        });
    }
}
