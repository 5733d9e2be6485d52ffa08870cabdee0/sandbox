package com.redhat.service.smartevents.infra.models;

import org.junit.jupiter.api.Test;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.infra.models.processors.ProcessorType.SINK;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryProcessorFilterInfoTest {

    @Test
    void testBuilder() {
        QueryProcessorFilterInfo query = QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder
                .filter().by("name").by(ACCEPTED).by(READY).by(SINK).build();

        assertThat(query.getFilterPrefix()).isEqualTo("name");
        assertThat(query.getFilterStatus()).containsOnly(ACCEPTED, READY);
        assertThat(query.getFilterType()).isEqualTo(SINK);
    }
}
