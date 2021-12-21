package com.redhat.service.bridge.manager.connectors;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class ConnectorsServiceTest {

    @Inject
    ConnectorsService connectorsService;

    @InjectMock
    ConnectorsApiClient connectorsApiClient;

    @Test
    @Transactional
    void doNotCreateConnector() {
        Optional<ConnectorEntity> connector = connectorsService.createConnectorIfNeeded(null, null, new WebhookAction());
        assertThat(connector).isEmpty();

        verify(connectorsApiClient, never()).createConnector(any());
    }
}
