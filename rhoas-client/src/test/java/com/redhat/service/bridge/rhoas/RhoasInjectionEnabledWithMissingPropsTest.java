package com.redhat.service.bridge.rhoas;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledWithMissingPropsTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@TestProfile(RhoasEnabledWithMissingPropsTestProfile.class)
class RhoasInjectionEnabledWithMissingPropsTest {

    @ConfigProperty(name = "event-bridge.feature-flags.rhoas-enabled")
    String enabled;

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(enabled).isEqualTo("true");
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> rhoasClient.get().toString());
    }

}
