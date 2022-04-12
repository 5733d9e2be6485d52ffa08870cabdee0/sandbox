package com.redhat.service.rhose.shard.operator.resources;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomResourceStatusTest {

    @Test
    public void testMarkConditionReady() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(ConditionType.Ready);
        resourceStatus.markConditionTrue(ConditionType.Augmentation);

        // Then
        assertThat(resourceStatus.isReady()).isTrue();
    }

    @Test
    public void testMarkConditionFailure() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionFalse(ConditionType.Ready, ConditionReason.DeploymentFailed, "");
        resourceStatus.markConditionTrue(ConditionType.Augmentation);

        // Then
        assertThat(resourceStatus.isReady()).isFalse();
        assertThat(resourceStatus.getConditionByType(ConditionType.Ready)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getReason()).isEqualTo(ConditionReason.DeploymentFailed);
        });
    }
}
