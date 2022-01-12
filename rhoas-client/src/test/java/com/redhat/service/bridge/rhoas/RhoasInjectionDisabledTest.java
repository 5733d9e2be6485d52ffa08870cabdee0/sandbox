package com.redhat.service.bridge.rhoas;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.rhoas.testprofiles.RhoasDisabledTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(RhoasDisabledTestProfile.class)
class RhoasInjectionDisabledTest {

    @ConfigProperty(name = "event-bridge.feature-flags.rhoas-enabled")
    String enabled;

    @Inject
    RhoasClient rhoasClient;

    @Test
    void test() {
        assertThat(enabled).isEqualTo("false");
        assertThat(rhoasClient).isNull();
    }

}
