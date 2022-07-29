package com.redhat.service.smartevents.manager.providers;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.TestConstants;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class TlsCertificateProviderTest {

    @Inject
    TlsCertificateProvider tlsCertificateProvider;

    @Test
    public void testCertificateProvider() {
        assertThat(tlsCertificateProvider.getTlsCertificate()).isEqualTo(TestConstants.DEFAULT_BRIDGE_TLS_CERTIFICATE);
        assertThat(tlsCertificateProvider.getTlsKey()).isEqualTo(TestConstants.DEFAULT_BRIDGE_TLS_KEY);
    }
}
