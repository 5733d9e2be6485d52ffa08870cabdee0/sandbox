package com.redhat.service.smartevents.manager.v1.services;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v1.api.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.api.models.responses.ProcessingErrorResponse;
import com.redhat.service.smartevents.manager.core.persistence.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.ProcessingError;
import com.redhat.service.smartevents.manager.core.providers.GlobalResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.RHOSE_BRIDGE_ID_HEADER;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CLIENT_ID;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CLIENT_SECRET;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorService.ENDPOINT_ERROR_HANDLER_TYPE;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorServiceImpl.ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_BROKER_URL;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorServiceImpl.ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_ID;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorServiceImpl.ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_SECRET;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorServiceImpl.ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_TOPIC;
import static com.redhat.service.smartevents.manager.v1.services.ProcessingErrorServiceImpl.ENDPOINT_RESOLVED_ERROR_HANDLER_TYPE;
import static com.redhat.service.smartevents.manager.v1.utils.TestUtils.createWebhookAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessingErrorServiceImplTest {

    public static String TEST_MANAGER_URL = "http://manager.example.com";
    public static String TEST_BOOTSTRAP_SERVERS = "kafka.example.com:1234";
    public static String TEST_CLIENT_ID = DEFAULT_CLIENT_ID;
    public static String TEST_CLIENT_SECRET = DEFAULT_CLIENT_SECRET;
    public static String TEST_GLOBAL_ERROR_TOPIC_NAME = "global-err";

    ProcessingErrorServiceImpl processingErrorService;

    InternalKafkaConfigurationProvider internalKafkaConfigurationProviderMock;
    GlobalResourceNamesProvider globalResourceNamesProviderMock;
    ProcessingErrorDAO processingErrorDAOMock;
    BridgesService bridgesServiceMock;

    @BeforeEach
    void beforeEach() {
        internalKafkaConfigurationProviderMock = mock(InternalKafkaConfigurationProvider.class);
        when(internalKafkaConfigurationProviderMock.getBootstrapServers()).thenReturn(TEST_BOOTSTRAP_SERVERS);
        when(internalKafkaConfigurationProviderMock.getClientId()).thenReturn(TEST_CLIENT_ID);
        when(internalKafkaConfigurationProviderMock.getClientSecret()).thenReturn(TEST_CLIENT_SECRET);

        globalResourceNamesProviderMock = mock(GlobalResourceNamesProvider.class);
        when(globalResourceNamesProviderMock.getGlobalErrorTopicName()).thenReturn(TEST_GLOBAL_ERROR_TOPIC_NAME);

        processingErrorDAOMock = mock(ProcessingErrorDAO.class);
        bridgesServiceMock = mock(BridgesService.class);

        processingErrorService = new ProcessingErrorServiceImpl();
        processingErrorService.eventBridgeManagerUrl = TEST_MANAGER_URL;
        processingErrorService.internalKafkaConfigurationProvider = internalKafkaConfigurationProviderMock;
        processingErrorService.resourceNamesProvider = globalResourceNamesProviderMock;
        processingErrorService.processingErrorDAO = processingErrorDAOMock;
        processingErrorService.bridgesService = bridgesServiceMock;
    }

    @Test
    void testGetProcessingErrors() {
        Action errorHandler = new Action();
        errorHandler.setType(ENDPOINT_ERROR_HANDLER_TYPE);

        BridgeDefinition bridgeDefinition = new BridgeDefinition();
        bridgeDefinition.setErrorHandler(errorHandler);

        Bridge bridge = new Bridge();
        bridge.setId(DEFAULT_BRIDGE_ID);
        bridge.setDefinition(bridgeDefinition);

        when(bridgesServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);

        QueryPageInfo queryInfo = new QueryPageInfo();

        assertThatNoException().isThrownBy(
                () -> processingErrorService.getProcessingErrors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, queryInfo));

        verify(processingErrorDAOMock).findByBridgeIdOrdered(DEFAULT_BRIDGE_ID, queryInfo);
    }

    @Test
    void testGetProcessingErrorsNotFound() {
        when(bridgesServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID))
                .thenThrow(new ItemNotFoundException("Item not found"));

        QueryPageInfo queryInfo = new QueryPageInfo();

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(
                () -> processingErrorService.getProcessingErrors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, queryInfo));

        verify(processingErrorDAOMock, never()).findByBridgeIdOrdered(any(), any());
    }

    @Test
    void testGetProcessingErrorsBadRequest() {
        BridgeDefinition bridgeDefinition = new BridgeDefinition();
        bridgeDefinition.setErrorHandler(createWebhookAction());

        Bridge bridge = new Bridge();
        bridge.setId(DEFAULT_BRIDGE_ID);
        bridge.setDefinition(bridgeDefinition);

        when(bridgesServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);

        QueryPageInfo queryInfo = new QueryPageInfo();

        assertThatExceptionOfType(BadRequestException.class).isThrownBy(
                () -> processingErrorService.getProcessingErrors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, queryInfo));

        verify(processingErrorDAOMock, never()).findByBridgeIdOrdered(any(), any());
    }

    @Test
    void testResolveAndUpdateErrorHandler() {
        Action errorHandler = new Action();
        errorHandler.setType(ENDPOINT_ERROR_HANDLER_TYPE);

        Action resolvedErrorHandler = processingErrorService.resolveAndUpdateErrorHandler(DEFAULT_BRIDGE_ID, errorHandler);

        assertThat(resolvedErrorHandler.getType())
                .isEqualTo(ENDPOINT_RESOLVED_ERROR_HANDLER_TYPE);
        assertThat(resolvedErrorHandler.getParameter(ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_TOPIC))
                .isEqualTo(TEST_GLOBAL_ERROR_TOPIC_NAME);
        assertThat(resolvedErrorHandler.getParameter(ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_BROKER_URL))
                .isEqualTo(TEST_BOOTSTRAP_SERVERS);
        assertThat(resolvedErrorHandler.getParameter(ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_ID))
                .isEqualTo(TEST_CLIENT_ID);
        assertThat(resolvedErrorHandler.getParameter(ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_SECRET))
                .isEqualTo(TEST_CLIENT_SECRET);

        String expectedErrorEndpoint = TEST_MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH + DEFAULT_BRIDGE_ID + "/errors";

        assertThat(errorHandler.getParameter("endpoint")).isEqualTo(expectedErrorEndpoint);
    }

    @Test
    void testResolveAndUpdateErrorHandlerWithNonEndpointErrorHandler() {
        Action errorHandler = createWebhookAction();
        Action resolvedErrorHandler = processingErrorService.resolveAndUpdateErrorHandler(DEFAULT_BRIDGE_ID, errorHandler);
        assertThat(resolvedErrorHandler).isSameAs(errorHandler);
    }

    @Test
    void testToResponse() {
        ZonedDateTime recordedAt = ZonedDateTime.of(2022, 2, 24, 4, 50, 0, 0, ZoneId.systemDefault());
        Map<String, String> headers = Map.of(RHOSE_BRIDGE_ID_HEADER, "123");
        ObjectNode payload = new ObjectNode(JsonNodeFactory.instance);

        ProcessingError processingError = new ProcessingError();
        processingError.setBridgeId(DEFAULT_BRIDGE_ID);
        processingError.setRecordedAt(recordedAt);
        processingError.setHeaders(headers);
        processingError.setPayload(payload);

        ProcessingErrorResponse response = processingErrorService.toResponse(processingError);

        assertThat(response.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(response.getHeaders()).isEqualTo(headers);
        assertThat(response.getPayload()).isEqualTo(payload);

    }

}
