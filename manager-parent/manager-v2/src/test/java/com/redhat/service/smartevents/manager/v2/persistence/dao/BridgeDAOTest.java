package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class BridgeDAOTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ConditionDAO conditionDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    @Transactional
    public void testStoreBridge() {
        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).isNull();
    }

    @Test
    @Transactional
    public void testStoreBridgeWithCondition() {
        Bridge bridge = createBridge();

        Condition condition = createCondition();

        bridge.setConditions(List.of(condition));
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).hasSize(1);
    }

    @Test
    @Transactional
    public void testStoreBridgeWithConditions() {
        Bridge bridge = createBridge();

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();

        bridge.setConditions(List.of(condition1, condition2, condition3));
        bridgeDAO.persist(bridge);

        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getOperation()).isNotNull();
        assertThat(retrieved.getOperation().getType()).isNotNull();
        assertThat(retrieved.getOperation().getRequestedAt()).isNotNull();
        assertThat(retrieved.getConditions()).hasSize(3);
    }

    @Test
    public void testRemovalOfCondition() {
        Bridge bridge = createBridgeWithConditions();
        removeConditionFromBridge(bridge);

        List<Condition> conditions = conditionDAO.findAll().list();

        assertThat(conditions.size()).isEqualTo(2);
    }

    @Transactional
    protected Bridge createBridgeWithConditions() {
        Bridge bridge = createBridge();

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();

        bridge.setConditions(List.of(condition1, condition2, condition3));
        bridgeDAO.persist(bridge);

        return bridge;
    }

    @Transactional
    protected void removeConditionFromBridge(Bridge bridge) {
        Bridge retrieved = bridgeDAO.findById(bridge.getId());
        assertThat(retrieved.getConditions()).hasSize(3);

        Condition condition3 = retrieved.getConditions().get(2);
        retrieved.getConditions().remove(condition3);
        bridgeDAO.persist(retrieved);
    }
}
