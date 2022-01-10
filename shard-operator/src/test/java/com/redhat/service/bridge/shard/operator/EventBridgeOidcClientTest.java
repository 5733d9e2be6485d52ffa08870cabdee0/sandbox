package com.redhat.service.bridge.shard.operator;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(KeycloakResource.class)
public class EventBridgeOidcClientTest {

    @Inject
    EventBridgeOidcClient eventBridgeOidcClient;

    @Test
    public void tokensAreRetrieved() {
        assertThat(eventBridgeOidcClient.getToken()).isNotBlank();
    }
}
