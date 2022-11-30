package com.redhat.service.smartevents.manager.v2.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StatusUtilitiesTest {

    @Test
    public void testManagedResourceWithEmptyConditions() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNullConditions() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        bridge.setConditions(null);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNoManagerCondition() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManagedResourceWithNoShardCondition() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThatThrownBy(() -> StatusUtilities.getManagedResourceStatus(bridge)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAcceptedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.UNKNOWN));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.ACCEPTED);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.ACCEPTED);
    }

    @Test
    public void testFailedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.UNKNOWN));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.FAILED));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.FAILED);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.FAILED);

        bridge.setOperation(new Operation(OperationType.DELETE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.FAILED);
    }

    @Test
    public void testPreparingManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.UNKNOWN));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PREPARING);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PREPARING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.FALSE));
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.UNKNOWN));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PREPARING);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PREPARING);
    }

    @Test
    public void testDeletingManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.DELETE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.DELETING);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.FALSE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.DELETING);
    }

    @Test
    public void testDeletedManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.DELETE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.TRUE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.DELETED);

        conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.TRUE));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.DELETED);
    }

    @Test
    public void testProvisioningManagedResource() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PROVISIONING);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PROVISIONING);
    }

    @Test
    public void testProvisioningManagedResourceWithoutManagerConditions() {
        Bridge bridge = new Bridge();
        bridge.setOperation(new Operation(OperationType.CREATE, null));
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createComponentConditionWithStatus(ComponentType.MANAGER, ConditionStatus.TRUE));
        conditions.add(createComponentConditionWithStatus(ComponentType.SHARD, ConditionStatus.UNKNOWN));
        bridge.setConditions(conditions);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PROVISIONING);

        bridge.setOperation(new Operation(OperationType.UPDATE, null));
        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatus.PROVISIONING);
    }

    private Condition createComponentConditionWithStatus(ComponentType componentType, ConditionStatus conditionStatus) {
        Condition condition = new Condition();
        condition.setComponent(componentType);
        condition.setStatus(conditionStatus);
        return condition;
    }
}
