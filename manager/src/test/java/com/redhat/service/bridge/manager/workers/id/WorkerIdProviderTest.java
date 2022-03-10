package com.redhat.service.bridge.manager.workers.id;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerIdProviderTest {

    @Test
    public void checkConsistency() {
        assertThat(new WorkerIdProvider().getWorkerId()).isEqualTo(new WorkerIdProvider().getWorkerId());
    }

}
