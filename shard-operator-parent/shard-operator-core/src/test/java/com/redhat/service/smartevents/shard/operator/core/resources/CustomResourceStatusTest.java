package com.redhat.service.smartevents.shard.operator.core.resources;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus.False;
import static com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus.True;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomResourceStatusTest {

    @Test
    public void testMarkConditionReady() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionTrue(ConditionTypeConstants.READY);
        resourceStatus.markConditionTrue(FooResourceStatus.AUGMENTATION);

        // Then
        assertThat(resourceStatus.isReady()).isTrue();
    }

    @Test
    public void testMarkConditionFailure() {
        // Given
        final CustomResourceStatus resourceStatus = new FooResourceStatus();

        // When
        resourceStatus.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_FAILED, "");
        resourceStatus.markConditionTrue(FooResourceStatus.AUGMENTATION);

        // Then
        assertThat(resourceStatus.isReady()).isFalse();
        assertThat(resourceStatus.getConditionByType(ConditionTypeConstants.READY)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getReason()).isEqualTo(ConditionReasonConstants.DEPLOYMENT_FAILED);
        });
    }

    private static Stream<Arguments> inferManagedResourceStatusParams() {
        return Stream.of(
                Arguments.of(True, null, READY),
                Arguments.of(False, False, FAILED),
                Arguments.of(True, False, READY),
                Arguments.of(False, True, PROVISIONING));
    }

}
