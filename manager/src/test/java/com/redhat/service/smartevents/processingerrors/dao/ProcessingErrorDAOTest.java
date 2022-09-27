package com.redhat.service.smartevents.processingerrors.dao;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.persistence.v1.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.persistence.v1.models.Bridge;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.models.ManagedResourceStatus.ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class ProcessingErrorDAOTest {

    public static final String TEST_1_BRIDGE_1_ID = "test-1-bridge-1";
    public static final String TEST_1_BRIDGE_2_ID = "test-1-bridge-2";
    public static final String TEST_2_BRIDGE_1_ID = "test-2-bridge-1";
    public static final String TEST_2_BRIDGE_2_ID = "test-2-bridge-2";

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessingErrorDAO processingErrorDAO;

    @Test
    void testCleanup() {
        bridgeDAO.persist(buildBridge(TEST_1_BRIDGE_1_ID));
        bridgeDAO.persist(buildBridge(TEST_1_BRIDGE_2_ID));

        IntStream.rangeClosed(1, 20)
                .mapToObj(i -> buildProcessingError(TEST_1_BRIDGE_1_ID))
                .forEach(processingErrorDAO::persist);

        long maxId1 = processingErrorDAO.findByBridgeIdOrdered(TEST_1_BRIDGE_1_ID, new QueryResourceInfo())
                .getItems().get(0).getId();

        IntStream.rangeClosed(1, 10)
                .mapToObj(i -> buildProcessingError(TEST_1_BRIDGE_2_ID))
                .forEach(processingErrorDAO::persist);

        long maxId2 = processingErrorDAO.findByBridgeIdOrdered(TEST_1_BRIDGE_2_ID, new QueryResourceInfo())
                .getItems().get(0).getId();

        processingErrorDAO.cleanup(5);

        ListResult<ProcessingError> errorBridge1 = processingErrorDAO
                .findByBridgeIdOrdered(TEST_1_BRIDGE_1_ID, new QueryResourceInfo());

        assertThat(errorBridge1.getTotal()).isEqualTo(5);
        long[] actualIds1 = errorBridge1.getItems().stream().mapToLong(ProcessingError::getId).sorted().toArray();
        assertThat(actualIds1).hasSize(5).contains(maxId1, maxId1 - 1, maxId1 - 2, maxId1 - 3, maxId1 - 4);

        ListResult<ProcessingError> errorBridge2 = processingErrorDAO
                .findByBridgeIdOrdered(TEST_1_BRIDGE_2_ID, new QueryResourceInfo());

        assertThat(errorBridge2.getTotal()).isEqualTo(5);
        long[] actualIds2 = errorBridge2.getItems().stream().mapToLong(ProcessingError::getId).sorted().toArray();
        assertThat(actualIds2).hasSize(5).contains(maxId2, maxId2 - 1, maxId2 - 2, maxId2 - 3, maxId2 - 4);
    }

    @Test
    void testCascadeDelete() {
        bridgeDAO.persist(buildBridge(TEST_2_BRIDGE_1_ID));
        bridgeDAO.persist(buildBridge(TEST_2_BRIDGE_2_ID));

        IntStream.rangeClosed(1, 20)
                .mapToObj(i -> buildProcessingError(TEST_2_BRIDGE_1_ID))
                .forEach(processingErrorDAO::persist);

        assertThat(processingErrorDAO.findByBridgeIdOrdered(TEST_2_BRIDGE_1_ID, new QueryResourceInfo()).getTotal())
                .isEqualTo(20);

        IntStream.rangeClosed(1, 10)
                .mapToObj(i -> buildProcessingError(TEST_2_BRIDGE_2_ID))
                .forEach(processingErrorDAO::persist);

        assertThat(processingErrorDAO.findByBridgeIdOrdered(TEST_2_BRIDGE_2_ID, new QueryResourceInfo()).getTotal())
                .isEqualTo(10);

        bridgeDAO.deleteById(TEST_2_BRIDGE_1_ID);

        assertThat(processingErrorDAO.findByBridgeIdOrdered(TEST_2_BRIDGE_1_ID, new QueryResourceInfo()).getTotal())
                .isZero();
        assertThat(processingErrorDAO.findByBridgeIdOrdered(TEST_2_BRIDGE_2_ID, new QueryResourceInfo()).getTotal())
                .isEqualTo(10);
    }

    private Bridge buildBridge(String id) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setId(id);
        bridge.setName("br-" + id);
        bridge.setStatus(ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setShardId(TestConstants.SHARD_ID);
        return bridge;
    }

    private ProcessingError buildProcessingError(String bridgeId) {
        ProcessingError processingError = new ProcessingError();
        processingError.setBridgeId(bridgeId);
        processingError.setRecordedAt(ZonedDateTime.now());
        processingError.setHeaders(Collections.emptyMap());
        return processingError;
    }
}
