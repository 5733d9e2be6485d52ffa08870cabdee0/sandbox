package com.redhat.service.smartevents.external.ansiblegateway;

import java.util.Base64;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class AnsibleTowerHeaderFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "ansible.tower.auth.basic.username")
    String basicAuthUsername;

    @ConfigProperty(name = "ansible.tower.auth.basic.password")
    String basicAuthPassword;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Authorization", "Basic " + getBasicAuthHeaderString());
        return result;
    }

    private String getBasicAuthHeaderString() {
        String basicAuthCredentials = basicAuthUsername + ":" + basicAuthPassword;
        return Base64.getEncoder().encodeToString(basicAuthCredentials.getBytes());
    }
}
