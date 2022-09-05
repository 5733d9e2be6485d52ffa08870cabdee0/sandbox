package com.redhat.service.smartevents.processingerrors.dao;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class ProcessingErrorDAOTest {

    @Inject
    ProcessingErrorDAO processingErrorDAO;

    @Test
    void testCleanup() {
        IntStream.rangeClosed(1, 20).mapToObj(i -> {
            ProcessingError processingError = new ProcessingError();
            processingError.setBridgeId("test-bridge-1");
            processingError.setRecordedAt(ZonedDateTime.now());
            processingError.setHeaders(Map.of("range-index", "" + i));
            return processingError;
        }).forEach(processingErrorDAO::persist);

        IntStream.rangeClosed(1, 10).mapToObj(i -> {
            ProcessingError processingError = new ProcessingError();
            processingError.setBridgeId("test-bridge-2");
            processingError.setRecordedAt(ZonedDateTime.now());
            processingError.setHeaders(Map.of("range-index", "" + i));
            return processingError;
        }).forEach(processingErrorDAO::persist);

        processingErrorDAO.cleanup(5);

        ListResult<ProcessingError> errorBridge1 = processingErrorDAO
                .findByBridgeIdOrdered("test-bridge-1", new QueryResourceInfo());

        assertThat(errorBridge1.getTotal()).isEqualTo(5);
        long[] actualIds1 = errorBridge1.getItems().stream().mapToLong(ProcessingError::getId).sorted().toArray();
        assertThat(actualIds1).hasSize(5).contains(16, 17, 18, 19, 20);

        ListResult<ProcessingError> errorBridge2 = processingErrorDAO
                .findByBridgeIdOrdered("test-bridge-2", new QueryResourceInfo());

        assertThat(errorBridge2.getTotal()).isEqualTo(5);
        long[] actualIds2 = errorBridge2.getItems().stream().mapToLong(ProcessingError::getId).sorted().toArray();
        assertThat(actualIds2).hasSize(5).contains(26, 27, 28, 29, 30);
    }
}
