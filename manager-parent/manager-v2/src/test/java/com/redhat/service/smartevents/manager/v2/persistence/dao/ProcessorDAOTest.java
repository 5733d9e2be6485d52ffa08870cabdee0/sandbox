package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessor;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorDAOTest {

    @Inject
    ProcessorDAO processorDAO;

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
    public void testStoreProcessor() {

        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Processor processor = createProcessor(bridge);
        processorDAO.persist(processor);

        Processor retrieved = processorDAO.findById(processor.getId());
        assertThat(retrieved.getId()).isEqualTo(processor.getId());
        assertThat(retrieved.getBridge().getId()).isEqualTo(bridge.getId());
    }

    @Test
    @Transactional
    public void testStoreProcessorWithConditions() {

        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Processor processor = createProcessor(bridge);

        Condition condition = createCondition();
        processor.setConditions(List.of(condition));
        processorDAO.persist(processor);

        Processor retrieved = processorDAO.findById(processor.getId());
        assertThat(retrieved.getId()).isEqualTo(processor.getId());
        assertThat(retrieved.getBridge().getId()).isEqualTo(bridge.getId());
        assertThat(retrieved.getConditions()).hasSize(1);
    }
}
