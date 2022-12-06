package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorProvisioningConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorReadyConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyBridge;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorServiceImplTest {

    public static final String NEW_PROCESSOR_NAME = "My Processor";
    public static final String NON_EXISTING_BRIDGE_ID = "non-existing-bridge-id";
    public static final String NOT_READY_BRIDGE_ID = "not-ready-bridge-id";
    public static final String PROVISIONING_PROCESSOR_ID = "provisioning-processor-id";
    public static final String PROVISIONING_PROCESSOR_NAME = "provisioning-processor-name";
    public static final String FAILED_PROCESSOR_ID = "failed-processor-id";
    public static final String FAILED_PROCESSOR_NAME = "failed-processor-name";

    @Inject
    ProcessorService processorService;

    @InjectMock
    ProcessorDAO processorDAO;

    @InjectMock
    BridgeService bridgeServiceMock;

    @BeforeEach
    public void cleanUp() {
        reset(bridgeServiceMock);
        reset(processorDAO);

        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        assertThat(getManagedResourceStatus(bridge)).isEqualTo(READY);

        Processor processor = createProcessor(bridge, createProcessorReadyConditions());
        assertThat(getManagedResourceStatus(processor)).isEqualTo(READY);

        Processor provisioningProcessor = createProcessor(bridge, createProcessorProvisioningConditions());
        provisioningProcessor.setId(PROVISIONING_PROCESSOR_ID);
        provisioningProcessor.setName(PROVISIONING_PROCESSOR_NAME);
        assertThat(getManagedResourceStatus(provisioningProcessor)).isEqualTo(PROVISIONING);

        Processor failedProcessor = createProcessor(bridge, createFailedConditions());
        failedProcessor.setId(FAILED_PROCESSOR_ID);
        failedProcessor.setName(FAILED_PROCESSOR_NAME);
        assertThat(getManagedResourceStatus(failedProcessor)).isEqualTo(FAILED);

        when(bridgeServiceMock.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenReturn(bridge);
        when(bridgeServiceMock.getReadyBridge(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Bridge not ready"));
        when(bridgeServiceMock.getReadyBridge(not(or(eq(DEFAULT_BRIDGE_ID), eq(NOT_READY_BRIDGE_ID))), eq(DEFAULT_CUSTOMER_ID))).thenThrow(new ItemNotFoundException("Bridge not found"));

        when(processorDAO.findById(DEFAULT_PROCESSOR_ID)).thenReturn(processor);
        when(processorDAO.findById(PROVISIONING_PROCESSOR_ID)).thenReturn(provisioningProcessor);
        when(processorDAO.findByBridgeIdAndName(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_NAME)).thenReturn(processor);
    }

    @Test
    void testCreateProcessor_bridgeNotActive() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(NEW_PROCESSOR_NAME);
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> processorService.createProcessor(NOT_READY_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_bridgeDoesNotExist() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(NEW_PROCESSOR_NAME);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> processorService.createProcessor(NON_EXISTING_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_processorWithSameNameAlreadyExists() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName(DEFAULT_PROCESSOR_NAME);
        assertThatExceptionOfType(AlreadyExistingItemException.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateProcessor_whiteSpaceInName() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("   name   ");
        Processor processor = doTestCreateProcessor(request);
        assertThat(processor.getName()).isEqualTo("name");
    }

    @Test
    void testCreateProcessorOrganizationWithNoQuota() {
        ProcessorRequest request = new ProcessorRequest();
        request.setName("name");
        assertThatExceptionOfType(NoQuotaAvailable.class)
                .isThrownBy(() -> processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, "organisation_with_no_quota", request));
    }

    private Processor doTestCreateProcessor(ProcessorRequest request) {
        Processor processor = processorService.createProcessor(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request);
        doAssertProcessorCreation(processor, request);
        return processor;
    }

    private void doAssertProcessorCreation(Processor processor, ProcessorRequest request) {
        assertThat(processor.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(processor.getName()).isEqualTo(request.getName());
        assertThat(getManagedResourceStatus(processor)).isEqualTo(ACCEPTED);
        assertThat(processor.getSubmittedAt()).isNotNull();
        assertThat(processor.getDefinition()).isNotNull();

        ArgumentCaptor<Processor> processorCaptor1 = ArgumentCaptor.forClass(Processor.class);
        verify(processorDAO, times(1)).persist(processorCaptor1.capture());
        assertThat(processorCaptor1.getValue()).isEqualTo(processor);
    }

    @Test
    public void testToResponse() {
        Bridge bridge = createBridge();
        Processor processor = createProcessor(bridge);
        processor.setOwner("Owner");

        processor.setConditions(createFailedConditions());

        ProcessorResponse response = processorService.toResponse(processor);

        assertThat(response.getId()).isEqualTo(processor.getId());
        assertThat(response.getName()).isEqualTo(processor.getName());
        assertThat(response.getStatus()).isEqualTo(FAILED);
        assertThat(response.getPublishedAt()).isEqualTo(processor.getPublishedAt());
        assertThat(response.getSubmittedAt()).isEqualTo(processor.getSubmittedAt());
        assertThat(response.getModifiedAt()).isNull();
        assertThat(response.getOwner()).isEqualTo("Owner");
        assertThat(response.getHref()).contains(V2APIConstants.V2_USER_API_BASE_PATH, bridge.getId());
        assertThat(response.getStatusMessage()).contains(TestConstants.FAILED_CONDITION_ERROR_CODE, TestConstants.FAILED_CONDITION_ERROR_MESSAGE);
    }
}
