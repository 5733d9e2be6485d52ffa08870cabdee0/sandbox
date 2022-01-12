package com.redhat.service.bridge.rhoas;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasDisabledTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@TestProfile(RhoasDisabledTestProfile.class)
class RhoasInjectionDisabledTest {

    @ConfigProperty(name = "event-bridge.feature-flags.rhoas-enabled")
    String enabled;

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(enabled).isEqualTo("false");
        assertThat(rhoasClient.isUnsatisfied()).isTrue();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> rhoasClient.get());
    }

}
