package com.redhat.service.smartevents.infra.models;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.queries.QueryFilterInfo;

import static com.redhat.service.smartevents.infra.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.ManagedResourceStatus.READY;
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
