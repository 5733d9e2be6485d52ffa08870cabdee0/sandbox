package com.redhat.service.smartevents.infra.v1.api.models.queries;

import org.junit.jupiter.api.Test;

import static com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1.ACCEPTED;
import static com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1.READY;
import static com.redhat.service.smartevents.infra.v1.api.models.queries.QueryFilterInfo.QueryFilterInfoBuilder.filter;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryFilterInfoTest {

    @Test
    void testBuilder() {
        QueryFilterInfo query = filter().by("name").by(ACCEPTED).by(READY).build();

        assertThat(query.getFilterName()).isEqualTo("name");
        assertThat(query.getFilterStatus()).containsOnly(ACCEPTED, READY);
    }
}
