package com.redhat.service.smartevents.manager.dao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ProcessorDAOTest {

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

    private Processor createProcessor(Bridge bridge, String name) {
        Processor p = new Processor();
        p.setType(ProcessorType.SINK);
        p.setBridge(bridge);
        p.setName(name);
        p.setStatus(ManagedResourceStatus.ACCEPTED);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setPublishedAt(ZonedDateTime.now());
        p.setShardId(TestConstants.SHARD_ID);

        Action a = new Action();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        a.setParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(Collections.emptySet(), null, a);
        p.setDefinition(definition);

        processorDAO.persist(p);
        return p;
    }

    private Bridge createBridge() {
        Bridge b = new Bridge();
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(ManagedResourceStatus.READY);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        b.setShardId(TestConstants.SHARD_ID);

        bridgeDAO.persist(b);
        return b;
    }

    @Test
    public void findByBridgeIdAndName_noMatchingBridgeId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        assertThat(processorDAO.findByBridgeIdAndName("doesNotExist", p.getName())).isNull();
    }

    @Test
    public void findByBridgeIdAndName_noMatchingProcessorName() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        assertThat(processorDAO.findByBridgeIdAndName(b.getId(), "doesNotExist")).isNull();
    }

    @Test
    public void findByBridgeIdAndName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        Processor byBridgeIdAndName = processorDAO.findByBridgeIdAndName(b.getId(), p.getName());
        assertThat(byBridgeIdAndName).isNotNull();
        assertThat(byBridgeIdAndName.getName()).isEqualTo(p.getName());
        assertThat(byBridgeIdAndName.getBridge().getId()).isEqualTo(b.getId());
    }

    @Test
    @Transactional
    public void findProcessorsToBeDeployedOrDelete() {
        Bridge b = createBridge();
        //To be provisioned
        Processor p = createProcessor(b, "foo");
        p.setDependencyStatus(ManagedResourceStatus.READY);
        processorDAO.getEntityManager().merge(p);

        //Already provisioned
        Processor q = createProcessor(b, "bob");
        q.setStatus(ManagedResourceStatus.READY);
        q.setDependencyStatus(ManagedResourceStatus.READY);
        processorDAO.getEntityManager().merge(q);

        //To be de-provisioned
        Processor r = createProcessor(b, "frank");
        r.setStatus(ManagedResourceStatus.DEPROVISION);
        r.setDependencyStatus(ManagedResourceStatus.DELETED);
        processorDAO.getEntityManager().merge(r);

        List<Processor> processors = processorDAO.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);
        assertThat(processors).hasSize(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn("foo", "frank"));
    }

    @Test
    @Transactional
    public void findProcessorsToBeDeployedOrDeleteWithConnectors() {
        Bridge b = createBridge();

        //To be provisioned
        Processor withProvisionedConnectors = createProcessor(b, "withProvisionedConnectors");
        withProvisionedConnectors.setStatus(ManagedResourceStatus.ACCEPTED);
        withProvisionedConnectors.setDependencyStatus(ManagedResourceStatus.READY);
        processorDAO.getEntityManager().merge(withProvisionedConnectors);

        ConnectorEntity provisionedConnector = Fixtures.createSinkConnector(withProvisionedConnectors,
                ManagedResourceStatus.READY);
        provisionedConnector.setName("connectorProvisioned");
        processorDAO.getEntityManager().merge(provisionedConnector);

        //Not to be provisioned as Connector is not ready
        Processor nonProvisioned = createProcessor(b, "withUnprovisionedConnector");
        nonProvisioned.setStatus(ManagedResourceStatus.ACCEPTED);
        nonProvisioned.setDependencyStatus(ManagedResourceStatus.PROVISIONING);
        processorDAO.getEntityManager().merge(nonProvisioned);

        ConnectorEntity nonProvisionedConnector = Fixtures.createSinkConnector(nonProvisioned,
                ManagedResourceStatus.READY);
        nonProvisionedConnector.setName("nonProvisionedConnector");
        processorDAO.getEntityManager().merge(nonProvisionedConnector);

        // Not to be de-provisioned as there's a connector yet to be deleted
        Processor toBeDeleted = createProcessor(b, "notToBeDeletedYet");
        toBeDeleted.setStatus(ManagedResourceStatus.DEPROVISION);
        toBeDeleted.setDependencyStatus(ManagedResourceStatus.READY);
        processorDAO.getEntityManager().merge(nonProvisioned);

        ConnectorEntity toBeDeletedConnector = Fixtures.createSinkConnector(toBeDeleted,
                ManagedResourceStatus.ACCEPTED);
        toBeDeletedConnector.setName("toBeDeletedConnector");
        processorDAO.getEntityManager().merge(toBeDeletedConnector);

        List<Processor> processors = processorDAO.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);
        assertThat(processors.stream().map(Processor::getName)).contains("withProvisionedConnectors");
    }

    @Test
    public void findByIdBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(b.getId(), p.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(p.getId());
    }

    @Test
    public void findByIdBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(b.getId(), "doesntExist", b.getCustomerId());
        assertThat(found).isNull();
    }

    @Test
    public void findByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        Processor p1 = createProcessor(b, "bar");

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isEqualTo(2L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        listResult.getItems().forEach((px) -> assertThat(px.getId()).isIn(p.getId(), p1.getId()));
    }

    @Test
    public void findByBridgeIdAndCustomerId_noProcessors() {
        Bridge b = createBridge();
        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isZero();
        assertThat(listResult.getTotal()).isZero();
    }

    @Test
    public void findByBridgeIdAndCustomerId_pageOffset() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        Processor p1 = createProcessor(b, "bar");

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(1, 1));
        assertThat(listResult.getPage()).isEqualTo(1L);
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        assertThat(listResult.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    public void testCountByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        Processor p1 = createProcessor(b, "bar");

        Long result = processorDAO.countByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(2L);
    }
}
