package com.redhat.service.smartevents.manager.v2.utils;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionUtilitiesTest {

    @ParameterizedTest
    @MethodSource("isOperationCompleteParameters")
    void testIsOperationComplete(List<Condition> conditions, boolean isOperationComplete) {
        assertThat(ConditionUtilities.isOperationComplete(conditions)).isEqualTo(isOperationComplete);
    }

    private static Stream<Arguments> isOperationCompleteParameters() {
        Object[][] arguments = {
                { null, true },
                { List.of(), true },
                { Fixtures.createProcessorAcceptedConditions(), false },
                { Fixtures.createProcessorPreparingConditions(), false },
                { Fixtures.createProcessorProvisioningConditions(), false },
                { Fixtures.createProcessorDeprovisionConditions(), false },
                { Fixtures.createProcessorDeletingConditions(), false },
                { Fixtures.createProcessorReadyConditions(), true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("isOperationFailedParameters")
    void testIsOperationFailed(List<Condition> conditions, boolean isOperationFailed) {
        assertThat(ConditionUtilities.isOperationFailed(conditions)).isEqualTo(isOperationFailed);
    }

    private static Stream<Arguments> isOperationFailedParameters() {
        Object[][] arguments = {
                { null, false },
                { List.of(), false },
                { Fixtures.createProcessorAcceptedConditions(), false },
                { Fixtures.createProcessorPreparingConditions(), false },
                { Fixtures.createProcessorProvisioningConditions(), false },
                { Fixtures.createProcessorDeprovisionConditions(), false },
                { Fixtures.createProcessorDeletingConditions(), false },
                { Fixtures.createProcessorReadyConditions(), false },
                { Fixtures.createFailedConditions(), true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
