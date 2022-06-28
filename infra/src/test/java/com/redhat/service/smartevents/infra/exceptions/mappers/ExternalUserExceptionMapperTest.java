package com.redhat.service.smartevents.infra.exceptions.mappers;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.api.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorType;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.UnclassifiedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExternalUserExceptionMapperTest {

    private static final BridgeError BRIDGE_ERROR = new BridgeError(1, "code", "reason", BridgeErrorType.USER);

    private static final BridgeError MAPPED_ERROR = new BridgeError(2, "mapped-code", "mapped-reason", BridgeErrorType.USER);

    @Mock
    private BridgeErrorService bridgeErrorService;

    private ExternalUserExceptionMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new ExternalUserExceptionMapper();
        this.mapper.bridgeErrorService = bridgeErrorService;
    }

    @Test
    void testMappedException() {
        when(bridgeErrorService.getError(ItemNotFoundException.class)).thenReturn(Optional.of(MAPPED_ERROR));

        ErrorsResponse response = mapper.toResponse(new ItemNotFoundException("message")).readEntity(ErrorsResponse.class);
        assertThat(response.getItems()).hasSize(1);

        ErrorResponse error = response.getItems().get(0);
        assertThat(error.getId()).isEqualTo("2");
        assertThat(error.getCode()).isEqualTo("mapped-code");
        assertThat(error.getReason()).isEqualTo("message");
    }

    @Test
    void testUnMappedException() {
        when(bridgeErrorService.getError(ItemNotFoundException.class)).thenReturn(Optional.empty());
        when(bridgeErrorService.getError(UnclassifiedException.class)).thenReturn(Optional.of(BRIDGE_ERROR));

        ErrorsResponse response = mapper.toResponse(new ItemNotFoundException("unmapped-reason")).readEntity(ErrorsResponse.class);
        assertThat(response.getItems()).hasSize(1);

        ErrorResponse error = response.getItems().get(0);
        assertThat(error.getId()).isEqualTo("1");
        assertThat(error.getCode()).isEqualTo("code");
        assertThat(error.getReason()).isEqualTo("unmapped-reason");
    }

}
