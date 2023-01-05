package com.redhat.service.smartevents.manager.v1.persistence.dao;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.v1.api.models.queries.QueryProcessorResourceInfo;
import com.redhat.service.smartevents.manager.v1.TestConstants;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v1.utils.Fixtures;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1.ACCEPTED;
import static com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1.READY;
import static com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType.SINK;
import static com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType.SOURCE;
import static com.redhat.service.smartevents.infra.v1.api.models.queries.QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder.filter;
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
        return createProcessor(bridge, name, SINK);
    }

    private Processor createProcessor(Bridge bridge, String name, ProcessorType type) {
        Processor p = new Processor();
        p.setType(type);
        p.setBridge(bridge);
        p.setName(name);
        p.setStatus(ManagedResourceStatusV1.ACCEPTED);
        p.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setShardId(TestConstants.SHARD_ID);
        p.setOwner(TestConstants.DEFAULT_USER_NAME);

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

    private Bridge createBridge() {
        Bridge b = Fixtures.createBridge();
        b.setStatus(ManagedResourceStatusV1.READY);
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
        p.setStatus(ManagedResourceStatusV1.PREPARING);
        p.setDependencyStatus(ManagedResourceStatusV1.READY);
        processorDAO.getEntityManager().merge(p);

        //Already provisioned
        Processor q = createProcessor(b, "bob");
        q.setStatus(ManagedResourceStatusV1.READY);
        q.setDependencyStatus(ManagedResourceStatusV1.READY);
        processorDAO.getEntityManager().merge(q);

        //To be de-provisioned
        Processor r = createProcessor(b, "frank");
        r.setStatus(ManagedResourceStatusV1.DEPROVISION);
        r.setDependencyStatus(ManagedResourceStatusV1.DELETED);
        processorDAO.getEntityManager().merge(r);

        //In the process of being provisioned
        Processor s = createProcessor(b, "mary");
        s.setStatus(ManagedResourceStatusV1.PROVISIONING);
        s.setDependencyStatus(ManagedResourceStatusV1.READY);
        processorDAO.getEntityManager().merge(s);

        //In the process of being deleted
        Processor t = createProcessor(b, "sue");
        t.setStatus(ManagedResourceStatusV1.DELETING);
        t.setDependencyStatus(ManagedResourceStatusV1.DELETED);
        processorDAO.getEntityManager().merge(t);

        List<Processor> processors = processorDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(processors).hasSize(4);
        processors.forEach((px) -> assertThat(px.getName()).isIn("foo", "frank", "mary", "sue"));
    }

    @Test
    @Transactional
    public void findProcessorsToBeDeployedOrDeleteWithConnectors() {
        Bridge b = createBridge();

        //To be provisioned
        Processor withProvisionedConnectors = createProcessor(b, "withProvisionedConnectors");
        withProvisionedConnectors.setStatus(ManagedResourceStatusV1.PREPARING);
        withProvisionedConnectors.setDependencyStatus(ManagedResourceStatusV1.READY);
        processorDAO.getEntityManager().merge(withProvisionedConnectors);

        ConnectorEntity provisionedConnector = Fixtures.createSinkConnector(withProvisionedConnectors,
                ManagedResourceStatusV1.READY);
        provisionedConnector.setName("connectorProvisioned");
        processorDAO.getEntityManager().merge(provisionedConnector);

        //Not to be provisioned as Connector is not ready
        Processor nonProvisioned = createProcessor(b, "withUnprovisionedConnector");
        nonProvisioned.setStatus(ManagedResourceStatusV1.PREPARING);
        nonProvisioned.setDependencyStatus(ManagedResourceStatusV1.PROVISIONING);
        processorDAO.getEntityManager().merge(nonProvisioned);

        ConnectorEntity nonProvisionedConnector = Fixtures.createSinkConnector(nonProvisioned,
                ManagedResourceStatusV1.PROVISIONING);
        nonProvisionedConnector.setName("nonProvisionedConnector");
        processorDAO.getEntityManager().merge(nonProvisionedConnector);

        // Not to be de-provisioned as there's a connector yet to be deleted
        Processor toBeDeleted = createProcessor(b, "notToBeDeletedYet");
        toBeDeleted.setStatus(ManagedResourceStatusV1.DEPROVISION);
        toBeDeleted.setDependencyStatus(ManagedResourceStatusV1.DELETING);
        processorDAO.getEntityManager().merge(nonProvisioned);

        ConnectorEntity toBeDeletedConnector = Fixtures.createSinkConnector(toBeDeleted,
                ManagedResourceStatusV1.DELETING);
        toBeDeletedConnector.setName("toBeDeletedConnector");
        processorDAO.getEntityManager().merge(toBeDeletedConnector);

        List<Processor> processors = processorDAO.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
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

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryProcessorResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isEqualTo(2L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        listResult.getItems().forEach((px) -> assertThat(px.getId()).isIn(p.getId(), p1.getId()));
    }

    @Test
    public void findByBridgeIdAndCustomerId_noProcessors() {
        Bridge b = createBridge();
        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryProcessorResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isZero();
        assertThat(listResult.getTotal()).isZero();
    }

    @Test
    public void findByBridgeIdAndCustomerId_pageOffset() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        createProcessor(b, "bar");

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryProcessorResourceInfo(1, 1));
        assertThat(listResult.getPage()).isEqualTo(1L);
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResult.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    public void testCountByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        createProcessor(b, "foo");
        createProcessor(b, "bar");

        Long result = processorDAO.countByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(result).isEqualTo(2L);
    }

    @Test
    void testGetProcessorsFilterByName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        createProcessor(b, "bar");

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getName()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    public void testGetProcessorsFilterByNameWildcard() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(b, "foo1");
        Processor p2 = createProcessor(b, "foo2");

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by("foo").build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(2L);
        assertThat(results.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default
        assertThat(results.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertThat(results.getItems().get(1).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByStatus() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        createProcessor(b, "bar");
        p.setStatus(READY);
        processorDAO.persist(p);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getStatus()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    void testGetProcessorsFilterByType() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo", SOURCE);
        createProcessor(b, "bar");

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getType()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByNameAndStatus() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        createProcessor(b, "bar");
        p.setStatus(READY);
        processorDAO.persist(p);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getName()).by(p.getStatus()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    void testGetProcessorsFilterByNameAndType() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo", SOURCE);
        createProcessor(b, "bar");

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getName()).by(p.getType()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByStatusAndType() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo", SOURCE);
        createProcessor(b, "bar");
        p.setStatus(READY);
        processorDAO.persist(p);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getStatus()).by(p.getType()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByNameAndStatusAndType() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo", SOURCE);
        createProcessor(b, "bar");
        p.setStatus(READY);
        processorDAO.persist(p);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(p.getName()).by(p.getStatus()).by(p.getType()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    public void testGetProcessorsFilterByMoreStatuses() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(b, "foo");
        p1.setStatus(ACCEPTED);
        processorDAO.persist(p1);
        Processor p2 = createProcessor(b, "bar");
        p2.setStatus(READY);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100, filter().by(ACCEPTED).by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(2L);
        assertThat(results.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default
        assertThat(results.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertThat(results.getItems().get(1).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsWithDefaultOrdering() {
        Bridge b = createBridge();

        IntStream.range(0, 5).forEach(i -> {
            Processor p = createProcessor(b, String.format("foo%s", i), SOURCE);
            p.setStatus(READY);
            processorDAO.persist(p);
        });

        ListResult<Processor> results = processorDAO.findUserVisibleByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryProcessorResourceInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(5L);
        assertThat(results.getTotal()).isEqualTo(5L);

        // Results are sorted descending by default. The first created is the last to be listed.
        IntStream.range(0, 5).forEach(i -> assertThat(results.getItems().get(4 - i).getName()).isEqualTo(String.format("foo%s", i)));
    }

}
