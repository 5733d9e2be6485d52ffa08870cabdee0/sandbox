package com.redhat.service.bridge.rhoas;

import java.util.NoSuchElementException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasEnabledWithMissingInstanceApiPropsTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.redhat.service.bridge.rhoas.RhoasProperties.INSTANCE_API_HOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

@QuarkusTest
@TestProfile(RhoasEnabledWithMissingInstanceApiPropsTestProfile.class)
class RhoasInjectionEnabledWithMissingInstanceApiPropsTest {

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatNoException().isThrownBy(() -> rhoasClient.get().toString());
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> rhoasClient.get().deleteTopic("foo-topic"))
                .withMessageContaining(INSTANCE_API_HOST);
    }
}
