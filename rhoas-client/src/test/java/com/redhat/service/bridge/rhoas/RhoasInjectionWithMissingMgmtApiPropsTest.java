package com.redhat.service.bridge.rhoas;

import java.util.NoSuchElementException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.kas.models.ServiceAccountRequest;
import com.redhat.service.bridge.test.rhoas.testprofiles.RhoasWithMissingMgmtApiPropsTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.redhat.service.bridge.rhoas.RhoasProperties.MGMT_API_HOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

@QuarkusTest
@TestProfile(RhoasWithMissingMgmtApiPropsTestProfile.class)
class RhoasInjectionWithMissingMgmtApiPropsTest {

    @Inject
    Instance<RhoasClient> rhoasClient;

    @Test
    void test() {
        assertThat(rhoasClient.isUnsatisfied()).isFalse();
        assertThat(rhoasClient.isAmbiguous()).isFalse();
        assertThatNoException().isThrownBy(() -> rhoasClient.get().toString());
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> rhoasClient.get().createServiceAccount(new ServiceAccountRequest().name("foo").description("bar")))
                .withMessageContaining(MGMT_API_HOST);
    }
}
