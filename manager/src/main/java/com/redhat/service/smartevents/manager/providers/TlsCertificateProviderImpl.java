package com.redhat.service.smartevents.manager.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TlsCertificateProviderImpl implements TlsCertificateProvider {

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.certificate")
    String certificate;

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.key")
    String key;

    @Override
    public String getTlsCertificate() {
        return certificate;
    }

    @Override
    public String getTlsKey() {
        return key;
    }
}
