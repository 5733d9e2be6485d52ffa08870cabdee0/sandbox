package com.redhat.service.bridge.manager.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.Fixtures;

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

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    public void before() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    private Processor createPersistProcessor(Bridge bridge) {
        Processor p = Fixtures.createProcessor(bridge, ManagedResourceStatus.ACCEPTED);

        BaseAction a = new BaseAction();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), null, a);
        p.setDefinition(mapper.valueToTree(definition));

        processorDAO.persist(p);
        return p;
    }

    private Bridge createPersistBridge() {
        Bridge b = Fixtures.createBridge();
        bridgeDAO.persist(b);
        return b;
    }

    private ConnectorEntity createPersistConnector(Processor p, ManagedResourceStatus status) {
        ConnectorEntity c = Fixtures.createConnector(p, status);
        connectorsDAO.persist(c);
        return c;
    }

    @Test
    public void findByProcessorIdName() {
        Bridge b = createPersistBridge();
        Processor p = createPersistProcessor(b);
        ConnectorEntity c = createPersistConnector(p, ManagedResourceStatus.READY);

        assertThat(connectorsDAO.findByProcessorIdAndName(p.getId(), c.getName())).isEqualTo(c);
    }

}
