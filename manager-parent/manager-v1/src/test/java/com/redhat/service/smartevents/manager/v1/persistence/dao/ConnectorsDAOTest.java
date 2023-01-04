package com.redhat.service.smartevents.manager.v1.persistence.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.v1.TestConstants;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v1.utils.Fixtures;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ConnectorsDAOTest {

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void before() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    private Processor createPersistProcessor(Bridge bridge) {
        Processor p = Fixtures.createProcessor(bridge, ManagedResourceStatusV1.ACCEPTED);

        Action a = new Action();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setMapParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), null, a);
        p.setDefinition(definition);

        processorDAO.persist(p);
        return p;
    }

    private Bridge createPersistBridge() {
        Bridge b = Fixtures.createBridge();
        bridgeDAO.persist(b);
        return b;
    }

    private ConnectorEntity createPersistConnector(Processor p, ManagedResourceStatusV1 status) {
        ConnectorEntity c = Fixtures.createSinkConnector(p, status);
        connectorsDAO.persist(c);
        return c;
    }

    @Test
    public void findByProcessorIdName() {
        Bridge b = createPersistBridge();
        Processor p = createPersistProcessor(b);
        ConnectorEntity c = createPersistConnector(p, ManagedResourceStatusV1.READY);

        assertThat(connectorsDAO.findByProcessorIdAndName(p.getId(), c.getName())).isEqualTo(c);
    }

}
