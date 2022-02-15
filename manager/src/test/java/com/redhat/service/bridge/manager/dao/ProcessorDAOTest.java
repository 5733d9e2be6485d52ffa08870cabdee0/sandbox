package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ProcessorDAOTest {

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
        p.setStatus(BridgeStatus.ACCEPTED);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setPublishedAt(ZonedDateTime.now());
        p.setShardId(TestConstants.SHARD_ID);

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
        b.setStatus(BridgeStatus.READY);
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
    public void findByStatuses() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        Processor q = createProcessor(b, "bob");
        q.setStatus(BridgeStatus.READY);
        processorDAO.getEntityManager().merge(q);

        Processor r = createProcessor(b, "frank");
        r.setStatus(BridgeStatus.DEPROVISION);
        processorDAO.getEntityManager().merge(r);

        List<Processor> processors = processorDAO.findByStatuses(asList(BridgeStatus.READY, BridgeStatus.DEPROVISION));
        assertThat(processors.size()).isEqualTo(2);
        processors.forEach((px) -> assertThat(px.getName()).isIn("bob", "frank"));
    }

    @Test
    public void findByIdBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(p.getId(), b.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(p.getId());
    }

    @Test
    public void findByIdBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId("doesntExist", b.getId(), b.getCustomerId());
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
