package com.redhat.service.smartevents.shard.operator.core;

import com.redhat.service.smartevents.test.resource.KeycloakResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class EventBridgeOidcClientTest {

    @Inject
    EventBridgeOidcClient eventBridgeOidcClient;

    @Test
    public void tokensAreRetrieved() {
        assertThat(eventBridgeOidcClient.getToken()).isNotBlank();
    }
}
