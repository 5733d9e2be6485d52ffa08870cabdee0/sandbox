package com.redhat.service.smartevents.infra.core.models;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.queries.QueryProcessorFilterInfo;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.infra.core.models.processors.ProcessorType.SINK;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryProcessorFilterInfoTest {

    @Test
    void testBuilder() {
        QueryProcessorFilterInfo query = QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder
                .filter().by("name").by(ACCEPTED).by(READY).by(SINK).build();

        assertThat(query.getFilterName()).isEqualTo("name");
        assertThat(query.getFilterStatus()).containsOnly(ACCEPTED, READY);
        assertThat(query.getFilterType()).isEqualTo(SINK);
    }
}
