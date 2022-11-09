package com.redhat.service.smartevents.infra.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BridgeErrorInstanceTest {

    @Test
    void testUUID() {
        BridgeError be = new BridgeError(1, "code", "reason", BridgeErrorType.USER);
        BridgeErrorInstance bei = new BridgeErrorInstance(be);
        assertThat(bei.getUuid()).isNotEmpty();
    }
}
