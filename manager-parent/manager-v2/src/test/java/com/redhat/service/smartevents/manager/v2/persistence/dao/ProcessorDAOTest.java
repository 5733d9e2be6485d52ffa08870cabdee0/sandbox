package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.infra.core.models.queries.QueryFilterInfo.QueryFilterInfoBuilder.filter;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createCondition;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorAcceptedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorReadyConditions;
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

    @Test
    public void testRemovalOfCondition() {
        Processor processor = createProcessorWithConditions();
        removeConditionFromProcessor(processor);

        List<Condition> conditions = conditionDAO.findAll().list();

        assertThat(conditions.size()).isEqualTo(2);
    }

    @Test
    public void testFindByIdWithConditions() {
        Processor processor = createProcessorWithConditions();

        Processor retrieved = processorDAO.findByIdWithConditions(processor.getId());

        assertThat(retrieved.getConditions().size()).isEqualTo(processor.getConditions().size());
    }

    @Transactional
    protected Processor createProcessorWithConditions() {
        Bridge bridge = createBridge();
        bridgeDAO.persist(bridge);

        Processor processor = createProcessor(bridge);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        Condition condition3 = createCondition();

        processor.setConditions(List.of(condition1, condition2, condition3));
        processorDAO.persist(processor);

        return processor;
    }

    @Transactional
    protected void removeConditionFromProcessor(Processor processor) {
        Processor retrieved = processorDAO.findById(processor.getId());
        assertThat(retrieved.getConditions()).hasSize(3);

        Condition condition3 = retrieved.getConditions().get(2);
        retrieved.getConditions().remove(condition3);
        processorDAO.persist(retrieved);
    }

    @Test
    @Transactional
    public void findByBridgeIdAndName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);
        processorDAO.persist(p);

        Processor byBridgeIdAndName = processorDAO.findByBridgeIdAndName(b.getId(), p.getName());
        assertThat(byBridgeIdAndName).isNotNull();
        assertThat(byBridgeIdAndName.getName()).isEqualTo(p.getName());
        assertThat(byBridgeIdAndName.getBridge().getId()).isEqualTo(b.getId());
    }

    @Test
    @Transactional
    public void findByBridgeIdAndName_noMatchingBridgeId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);
        processorDAO.persist(p);

        assertThat(processorDAO.findByBridgeIdAndName("doesNotExist", p.getName())).isNull();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndName_noMatchingProcessorName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);
        processorDAO.persist(p);

        assertThat(processorDAO.findByBridgeIdAndName(b.getId(), "doesNotExist")).isNull();
    }

    @Test
    @Transactional
    public void findByIdBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);
        processorDAO.persist(p);

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(b.getId(), p.getId(), b.getCustomerId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    public void findByIdBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);
        processorDAO.persist(p);

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(b.getId(), "doesntExist", b.getCustomerId());
        assertThat(found).isNull();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isEqualTo(2L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        listResult.getItems().forEach((px) -> assertThat(px.getId()).isIn(p1.getId(), p2.getId()));
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerId_noProcessors() {
        Bridge b = createBridge();
        bridgeDAO.persist(b);

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getSize()).isZero();
        assertThat(listResult.getTotal()).isZero();
    }

    @Test
    @Transactional
    public void findByBridgeIdAndCustomerId_pageOffset() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> listResult = processorDAO.findByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID, new QueryResourceInfo(1, 1));
        assertThat(listResult.getPage()).isEqualTo(1L);
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResult.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    public void testCountByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        long total = processorDAO.countByBridgeIdAndCustomerId(b.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(total).isEqualTo(2L);
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByName() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(p1.getName()).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    public void testGetProcessorsFilterByNameWildcard() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo1");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "foo2");
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by("foo").build()));
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
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        p1.setConditions(createProcessorReadyConditions());
        p2.setConditions(createProcessorAcceptedConditions());
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    void testGetProcessorsFilterByNameAndStatus() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        p1.setConditions(createProcessorReadyConditions());
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(p1.getName()).by(READY).build()));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(1L);
        assertThat(results.getTotal()).isEqualTo(1L);
        assertThat(results.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @Transactional
    public void testGetProcessorsFilterByMoreStatuses() {
        Bridge b = createBridge();
        Processor p1 = createProcessor(DEFAULT_PROCESSOR_ID + "1", b, "foo");
        p1.setConditions(createProcessorAcceptedConditions());
        Processor p2 = createProcessor(DEFAULT_PROCESSOR_ID + "2", b, "bar");
        p2.setConditions(createProcessorReadyConditions());
        bridgeDAO.persist(b);
        processorDAO.persist(p1);
        processorDAO.persist(p2);

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(),
                new QueryResourceInfo(0, 100, filter().by(ACCEPTED).by(READY).build()));
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
        bridgeDAO.persist(b);

        IntStream.range(0, 5).forEach(i -> {
            String id = String.format("id%s", i);
            String name = String.format("name%s", i);
            Processor p = createProcessor(id, b, name);
            p.setConditions(createProcessorReadyConditions());
            processorDAO.persist(p);
        });

        ListResult<Processor> results = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId(), new QueryResourceInfo(0, 100));
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(5L);
        assertThat(results.getTotal()).isEqualTo(5L);

        // Results are sorted descending by default. The first created is the last to be listed.
        IntStream.range(0, 5).forEach(i -> assertThat(results.getItems().get(4 - i).getId()).isEqualTo(String.format("id%s", i)));
    }

    @Test
    @Transactional
    public void findByShardIdToDeployOrDelete_WhenControlPlaneIsComplete() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();

        p.setConditions(List.of(condition1, condition2));
        processorDAO.persist(p);

        List<Processor> processors = processorDAO.findByShardIdToDeployOrDelete(b.getShardId());
        assertThat(processors).isNotNull();
        assertThat(processors).hasSize(1);
        assertThat(processors.get(0).getId()).isEqualTo(p.getId());
    }

    @Test
    @Transactional
    public void findByShardIdToDeployOrDelete_WhenControlPlaneIsIncomplete() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");
        bridgeDAO.persist(b);

        Condition condition1 = createCondition();
        Condition condition2 = createCondition();
        condition2.setStatus(ConditionStatus.UNKNOWN);

        p.setConditions(List.of(condition1, condition2));
        processorDAO.persist(p);

        List<Processor> processors = processorDAO.findByShardIdToDeployOrDelete(b.getShardId());
        assertThat(processors).isNotNull();
        assertThat(processors).isEmpty();
    }

}
