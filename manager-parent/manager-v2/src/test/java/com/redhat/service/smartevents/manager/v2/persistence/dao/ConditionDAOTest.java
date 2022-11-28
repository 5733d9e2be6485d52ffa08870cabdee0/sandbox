package com.redhat.service.smartevents.manager.v2.persistence.dao;

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
public class ConditionDAOTest {

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
    public void testStoreCondition() {
        Bridge bridge = createBridge();
        Condition condition = createCondition(bridge, null);

        bridgeDAO.persist(bridge);
        conditionDAO.persist(condition);

        Condition retrieved = conditionDAO.findById(condition.getId());
        assertThat(retrieved.getId()).isEqualTo(condition.getId());
        assertThat(retrieved.getBridge().getId()).isEqualTo(bridge.getId());
    }
}
