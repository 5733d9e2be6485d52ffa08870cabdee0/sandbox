package com.redhat.service.bridge.rhoas;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG;
import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG_DEFAULT_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class RhoasInjectionDefaultTest {

    @ConfigProperty(name = ENABLED_FLAG, defaultValue = ENABLED_FLAG_DEFAULT_VALUE)
    boolean rhoasEnabled;

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(rhoasEnabled).isFalse();
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> rhoasClient.get().toString())
                .withMessage(RhoasProducer.RHOAS_DISABLED_ERROR_MESSAGE);
    }
}
