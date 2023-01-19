package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;

import static com.redhat.service.smartevents.infra.v2.api.models.ComponentType.MANAGER;
import static com.redhat.service.smartevents.infra.v2.api.models.ComponentType.SHARD;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.FALSE;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.TRUE;
import static com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus.UNKNOWN;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.ACCEPTED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.PREPARING;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.PROVISIONING;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.READY;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.UPDATE_ACCEPTED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.UPDATE_PREPARING;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.UPDATE_PROVISIONING;
import static com.redhat.service.smartevents.infra.v2.api.models.OperationType.CREATE;
import static com.redhat.service.smartevents.infra.v2.api.models.OperationType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StatusUtilitiesTest {

    private static Stream<Arguments> getStatusMessageParameters() {
        Object[][] arguments = {
                { null, null },
                { List.of(), null },
                { Fixtures.createBridgeAcceptedConditions(), null },
                { Fixtures.createBridgeReadyConditions(), null },
                { Fixtures.createBridgeDeprovisionConditions(), null },
                { Fixtures.createProcessorAcceptedConditions(), null },
                { Fixtures.createProcessorPreparingConditions(), null },
                { Fixtures.createProcessorProvisioningConditions(), null },
                { Fixtures.createProcessorReadyConditions(), null },
                { Fixtures.createProcessorDeprovisionConditions(), null },
                { Fixtures.createProcessorDeletingConditions(), null },
                { List.of(createConditionWithErrorCodeAndMessage(null, "Failed")), null },
                { List.of(createConditionWithErrorCodeAndMessage("1", null)), List.of("[1]") },
                { List.of(createConditionWithErrorCodeAndMessage("1", "Failed")), List.of("[1] Failed") },
                { List.of(createConditionWithErrorCodeAndMessage("1", "Failed"), createConditionWithErrorCodeAndMessage("2", "Broken")), List.of("[1] Failed", "[2] Broken") },
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static Stream<Arguments> getIsActionableParameters() {
        Object[][] arguments = {
                { Fixtures.createProcessorAcceptedConditions(), false },
                { Fixtures.createProcessorPreparingConditions(), false },
                { Fixtures.createProcessorProvisioningConditions(), false },
                { Fixtures.createProcessorReadyConditions(), true },
                { Fixtures.createProcessorDeprovisionConditions(), false },
                { Fixtures.createProcessorDeletingConditions(), false },
                { Fixtures.createFailedConditions(), true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static Stream<Arguments> getManagerDependenciesCompletedParameters() {
        Object[][] arguments = {
                { null, false },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(SHARD, FALSE)), true },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(SHARD, UNKNOWN)), true },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(SHARD, ConditionStatus.FAILED)), true },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(SHARD, FALSE)), true },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(MANAGER, UNKNOWN), createComponentConditionWithStatus(SHARD, FALSE)), false },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(MANAGER, FALSE), createComponentConditionWithStatus(SHARD, FALSE)), false },
                { List.of(createComponentConditionWithStatus(MANAGER, TRUE), createComponentConditionWithStatus(MANAGER, ConditionStatus.FAILED), createComponentConditionWithStatus(SHARD, FALSE)),
                        false },
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    private static Condition createConditionWithErrorCodeAndMessage(String errorCode, String message) {
        Condition c = new Condition();
        c.setErrorCode(errorCode);
        c.setMessage(message);
        c.setStatus(ConditionStatus.FAILED);
        return c;
    }

    @Test
    public void testGetModifiedAt_Null() {
        assertThat(StatusUtilities.getModifiedAt(null)).isNull();
    }

    @Test
    public void testGetModifiedAt_NullOperation() {
        assertThat(StatusUtilities.getModifiedAt(new Bridge())).isNull();
    }

    @Test
    public void testGetModifiedAt_Created() {
        ManagedResourceV2 resource = new Bridge();
        Operation operation = new Operation();
        operation.setType(CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.setOperation(operation);
        assertThat(StatusUtilities.getModifiedAt(resource)).isNull();
    }

    @Test
    public void testGetModifiedAt_Deleted() {
        ManagedResourceV2 resource = new Bridge();
        Operation operation = new Operation();
        operation.setType(CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.setOperation(operation);
        assertThat(StatusUtilities.getModifiedAt(resource)).isNull();
    }

    @Test
    public void testGetModifiedAt_Updated() {
        ManagedResourceV2 resource = new Bridge();
        Operation operation = new Operation();
        operation.setType(UPDATE);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        operation.setRequestedAt(now);
        resource.setOperation(operation);
        assertThat(StatusUtilities.getModifiedAt(resource)).isEqualTo(now);
    }

    @Test
    public void testManagedResourceWithEmptyConditions() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNullConditions() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        bridge.setConditions(null);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNoManagerCondition() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNoShardCondition() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAcceptedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ACCEPTED);

        bridge.setOperation(new Operation(UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(UPDATE_ACCEPTED);
    }

    @Test
    public void testReadyManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, TRUE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(READY);

        bridge.setOperation(new Operation(UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(READY);
    }

    @Test
    public void testFailedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, ConditionStatus.FAILED));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.FAILED);

        bridge.setOperation(new Operation(UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.FAILED);

        bridge.setOperation(new Operation(OperationType.DELETE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.FAILED);
    }

    @Test
    public void testPreparingManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, FALSE));
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(PREPARING);

        bridge.setOperation(new Operation(UPDATE, null));
        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.UPDATE_PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, FALSE));
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.UPDATE_PREPARING);
    }

    @Test
    public void testProvisioningManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, FALSE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, FALSE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(PROVISIONING);

        bridge.setOperation(new Operation(UPDATE, null));
        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(UPDATE_PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, FALSE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(UPDATE_PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, FALSE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(UPDATE_PROVISIONING);
    }

    @Test
    public void testDeprovisioningManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.DELETE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, UNKNOWN));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.DEPROVISION);
    }

    @Test
    public void testDeletingManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.DELETE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.DEPROVISION);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, FALSE));
        conditions.add(createComponentConditionWithStatus(SHARD, UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.DEPROVISION);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, FALSE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.DELETING);
    }

    @Test
    public void testDeletedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.DELETE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(MANAGER, TRUE));
        conditions.add(createComponentConditionWithStatus(SHARD, TRUE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.DELETED);
    }

    @ParameterizedTest
    @MethodSource("getStatusMessageParameters")
    public void testGetStatusMessage(List<Condition> conditions, List<String> messages) {
        Bridge resource = new Bridge();
        resource.setConditions(conditions);

        String message = StatusUtilities.getStatusMessage(resource);
        if (Objects.isNull(messages)) {
            assertThat(message).isNull();
        } else {
            messages.forEach(m -> assertThat(message).contains(m));
        }
    }

    @ParameterizedTest
    @MethodSource("getManagerDependenciesCompletedParameters")
    public void testManagerDependenciesCompleted(List<Condition> conditions, boolean expectedResult) {
        Bridge resource = new Bridge();
        resource.setConditions(conditions);

        boolean result = StatusUtilities.managerDependenciesCompleted(resource);
        assertThat(result).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getIsActionableParameters")
    public void testGetIsActionableParameters(List<Condition> conditions, boolean isActionable) {
        Bridge resource = new Bridge();
        Operation operation = new Operation(CREATE, ZonedDateTime.now(ZoneOffset.UTC));
        resource.setOperation(operation);
        resource.setConditions(conditions);

        assertThat(StatusUtilities.isActionable(resource)).isEqualTo(isActionable);
    }

    private static Condition createComponentConditionWithStatus(ComponentType componentType, ConditionStatus conditionStatus) {
        Condition condition = new Condition();
        condition.setComponent(componentType);
        condition.setStatus(conditionStatus);
        return condition;
    }
}
