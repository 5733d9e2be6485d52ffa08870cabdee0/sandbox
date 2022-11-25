package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createOperation;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ConditionDAOTest {

    @Inject
    ConditionDAO conditionDAO;

    @Inject
    OperationDAO operationDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    @Transactional
    public void testStoreOperation() {

        Operation operation = createOperation(DEFAULT_BRIDGE_ID);
        Condition condition = createCondition(operation);
        condition.setOperation(operation);

        operationDAO.persist(operation);
        conditionDAO.persist(condition);

        Condition retrieved = conditionDAO.findById(condition.getId());
        assertThat(retrieved.getId()).isEqualTo(condition.getId());
        assertThat(retrieved.getOperation().getId()).isEqualTo(operation.getId());
    }
}
