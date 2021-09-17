package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
        databaseManagerUtils.cleanDatabase();
    }

    private Processor createProcessor(Bridge bridge, String name) {
        Processor p = new Processor();
        p.setBridge(bridge);
        p.setName(name);
        p.setStatus(BridgeStatus.REQUESTED);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setPublishedAt(ZonedDateTime.now());
        processorDAO.persist(p);
        return p;
    }

    private Bridge createBridge() {
        Bridge b = new Bridge();
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(BridgeStatus.AVAILABLE);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    @Test
    public void findByBridgeIdAndName_noMatchingBridgeId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        assertThat(processorDAO.findByBridgeIdAndName("doesNotExist", p.getName()), is(nullValue()));
    }

    @Test
    public void findByBridgeIdAndName_noMatchingProcessorName() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        assertThat(processorDAO.findByBridgeIdAndName(b.getId(), "doesNotExist"), is(nullValue()));
    }

    @Test
    public void findByBridgeIdAndName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        Processor byBridgeIdAndName = processorDAO.findByBridgeIdAndName(b.getId(), p.getName());
        assertThat(byBridgeIdAndName, is(notNullValue()));
        assertThat(byBridgeIdAndName.getName(), equalTo(p.getName()));
        assertThat(byBridgeIdAndName.getBridge().getId(), equalTo(b.getId()));
    }

    @Test
    @Transactional
    public void findByStatuses() {

        Bridge b = createBridge();
        createProcessor(b, "foo");

        Processor q = createProcessor(b, "bob");
        q.setStatus(BridgeStatus.AVAILABLE);
        processorDAO.getEntityManager().merge(q);

        Processor r = createProcessor(b, "frank");
        r.setStatus(BridgeStatus.DELETION_REQUESTED);
        processorDAO.getEntityManager().merge(r);

        List<Processor> processors = processorDAO.findByStatuses(asList(BridgeStatus.AVAILABLE, BridgeStatus.DELETION_REQUESTED));
        assertThat(processors, hasSize(2));
        processors.stream().forEach((px) -> assertThat(px.getName(), in(asList("bob", "frank"))));
    }

    @Test
    public void findByIdBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId(p.getId(), b.getId(), b.getCustomerId());
        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), equalTo(p.getId()));
    }

    @Test
    public void findByIdBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        Processor found = processorDAO.findByIdBridgeIdAndCustomerId("doesntExist", b.getId(), b.getCustomerId());
        assertThat(found, is(nullValue()));
    }

    @Test
    public void findByBridgeIdAndCustomerId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b, "foo");

        List<Processor> found = processorDAO.findByBridgeIdAndCustomerId(b.getId(), b.getCustomerId());
        assertThat(found.size(), equalTo(1));
        assertThat(found.get(0).getId(), equalTo(p.getId()));
    }

    @Test
    public void findByBridgeIdAndCustomerId_doesNotExist() {
        Bridge b = createBridge();
        createProcessor(b, "foo");

        List<Processor> found = processorDAO.findByBridgeIdAndCustomerId("doesntExist", b.getCustomerId());
        assertThat(found.size(), equalTo(0));
    }
}
