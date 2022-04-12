package com.redhat.service.rhose.rhoas;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@QuarkusTest
class RhoasInjectionDefaultTest {

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatNoException().isThrownBy(() -> rhoasClient.get().toString());
    }
}
