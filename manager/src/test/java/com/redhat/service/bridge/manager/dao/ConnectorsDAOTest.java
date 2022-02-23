package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ManagedEntityStatus;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
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

    private Processor createProcessor(Bridge bridge, String name) {
        Processor p = new Processor();
        p.setBridge(bridge);
        p.setName(name);
        p.setStatus(ManagedEntityStatus.ACCEPTED);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setPublishedAt(ZonedDateTime.now());

        BaseAction a = new BaseAction();
        a.setType(KafkaTopicAction.TYPE);
        a.setName(TestConstants.DEFAULT_ACTION_NAME);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), null, a);
        p.setDefinition(mapper.valueToTree(definition));

        processorDAO.persist(p);
        return p;
    }

    private Bridge createBridge() {
        Bridge b = new Bridge();
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(ManagedEntityStatus.READY);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    private ConnectorEntity createPersistConnector(Processor p, String connectorName,
            ConnectorStatus status,
            ConnectorStatus desiredStatus) {
        ConnectorEntity c = Fixtures.createConnector(p, connectorName, status, desiredStatus, "topicName");
        connectorsDAO.persist(c);
        return c;
    }

    @Test
    public void findByProcessorIdName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        ConnectorEntity c = createPersistConnector(p, "connector", ConnectorStatus.READY, ConnectorStatus.READY);

        assertThat(connectorsDAO.findByProcessorIdAndName(p.getId(), c.getName())).isEqualTo(c);
    }

    @Test
    public void findUnprocessed() {
        Bridge b = createBridge();

        Processor p1 = createProcessor(b, "availableConnectorProcessor");
        createPersistConnector(p1, "availableConnector", ConnectorStatus.READY, ConnectorStatus.READY);

        Processor p2 = createProcessor(b, "requestedConnectorProcessor");
        ConnectorEntity requestedConnector = createPersistConnector(p2, "availableConnector", ConnectorStatus.ACCEPTED, ConnectorStatus.READY);

        List<ConnectorEntity> unprocessedConnectors = connectorsDAO.findUnprocessed();

        assertThat(unprocessedConnectors).containsOnly(requestedConnector);
    }
}
