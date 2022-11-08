package com.redhat.service.smartevents.infra.core.models.queries;

import org.junit.jupiter.api.Test;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryFilterInfoTest {

    @Test
    void testBuilder() {
        QueryFilterInfo query = QueryFilterInfo.QueryFilterInfoBuilder
                .filter().by("name").by(ACCEPTED).by(READY).build();

        assertThat(query.getFilterName()).isEqualTo("name");
        assertThat(query.getFilterStatus()).containsOnly(ACCEPTED, READY);
    }
}
