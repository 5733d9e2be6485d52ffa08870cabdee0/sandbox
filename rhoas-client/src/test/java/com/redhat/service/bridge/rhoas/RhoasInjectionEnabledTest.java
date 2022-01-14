package com.redhat.service.bridge.rhoas;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG;
import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG_DEFAULT_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@QuarkusTest
@TestProfile(RhoasEnabledTestProfile.class)
class RhoasInjectionEnabledTest {

    @ConfigProperty(name = ENABLED_FLAG, defaultValue = ENABLED_FLAG_DEFAULT_VALUE)
    boolean rhoasEnabled;

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(rhoasEnabled).isTrue();
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatNoException().isThrownBy(() -> rhoasClient.get().toString());
    }
}
