package com.redhat.developer.manager.dao;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.Processor;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

    private Processor createProcessor(Bridge bridge) {
        Processor p = new Processor();
        bridge.addProcessor(p);
        p.setName("foo");
        p.setStatus(BridgeStatus.REQUESTED);
        p.setSubmittedAt(ZonedDateTime.now());
        p.setPublishedAt(ZonedDateTime.now());
        processorDAO.persist(p);
        return p;
    }

    private Bridge createBridge() {
        Bridge b = new Bridge();
        b.setName("foo-" + System.currentTimeMillis());
        b.setCustomerId("bar");
        b.setStatus(BridgeStatus.REQUESTED);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setPublishedAt(ZonedDateTime.now());
        bridgeDAO.persist(b);
        return b;
    }

    @Test
    public void findByBridgeIdAndName_noMatchingBridgeId() {
        Bridge b = createBridge();
        Processor p = createProcessor(b);

        assertThat(processorDAO.findByBridgeIdAndName("doesNotExist", p.getName()), is(nullValue()));
    }

    @Test
    public void findByBridgeIdAndName_noMatchingProcessorName() {
        Bridge b = createBridge();
        createProcessor(b);

        assertThat(processorDAO.findByBridgeIdAndName(b.getId(), "doesNotExist"), is(nullValue()));
    }

    @Test
    public void findByBridgeIdAndName() {
        Bridge b = createBridge();
        Processor p = createProcessor(b);

        Processor byBridgeIdAndName = processorDAO.findByBridgeIdAndName(b.getId(), p.getName());
        assertThat(byBridgeIdAndName, is(notNullValue()));
        assertThat(byBridgeIdAndName.getName(), equalTo(p.getName()));
        assertThat(byBridgeIdAndName.getBridge().getId(), equalTo(b.getId()));
    }
}
