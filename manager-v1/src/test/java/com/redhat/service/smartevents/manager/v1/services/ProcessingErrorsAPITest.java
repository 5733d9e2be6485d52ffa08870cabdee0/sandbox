package com.redhat.service.smartevents.manager.v1.services;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.manager.core.api.models.responses.ProcessingErrorListResponse;
import com.redhat.service.smartevents.manager.core.persistence.models.ProcessingError;
import com.redhat.service.smartevents.manager.v1.TestConstants;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v1.utils.TestUtils.jsonRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProcessingErrorsAPITest {

    @InjectMock
    ProcessingErrorService processingErrorService;
    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    public void beforeEach() {
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);

        reset(processingErrorService);
    }

    @Test
    void testWithNoAuthentication() {
        getProcessingErrors().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    void testWithBadRequest() {
        when(processingErrorService.getProcessingErrors(any(), any(), any()))
                .thenThrow(new BadRequestException("Bad request"));

        getProcessingErrors().then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    void testWithNoData() {
        when(processingErrorService.getProcessingErrors(any(), any(), any()))
                .thenReturn(new ListResult<>(Collections.emptyList()));

        ProcessingErrorListResponse response = getProcessingErrors().as(ProcessingErrorListResponse.class);

        assertThat(response.getKind()).isEqualTo(ProcessingErrorListResponse.KIND);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isZero();
        assertThat(response.getTotal()).isZero();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    void testWithData() {
        when(processingErrorService.getProcessingErrors(any(), any(), any()))
                .thenReturn(new ListResult<>(List.of(processingError(7), processingError(8)), 1, 12));

        ProcessingErrorListResponse response = getProcessingErrors().as(ProcessingErrorListResponse.class);

        assertThat(response.getKind()).isEqualTo(ProcessingErrorListResponse.KIND);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualTo(12);
        assertThat(response.getItems()).hasSize(2);
    }

    private static ProcessingError processingError(int hour) {
        ProcessingError processingError = new ProcessingError();
        processingError.setBridgeId(DEFAULT_BRIDGE_ID);
        processingError.setRecordedAt(ZonedDateTime.of(2022, 8, 29, hour, 0, 0, 0, ZoneId.systemDefault()));
        processingError.setHeaders(Collections.emptyMap());
        processingError.setPayload(new ObjectNode(JsonNodeFactory.instance));
        return processingError;
    }

    private static Response getProcessingErrors() {
        return jsonRequest()
                .get(V1APIConstants.V1_USER_API_BASE_PATH + DEFAULT_BRIDGE_ID + "/errors");
    }

}
