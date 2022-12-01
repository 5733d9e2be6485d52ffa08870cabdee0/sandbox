package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorType;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.VaultException;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InternalPlatformExceptionMapperTest {

    private static final BridgeError MAPPED_ERROR = new BridgeError(1, "mapped-code", "mapped-reason", BridgeErrorType.USER);

    @Mock
    private BridgeErrorService bridgeErrorService;

    private InternalPlatformExceptionMapper mapper;

    @BeforeEach
    void setup() {
        when(bridgeErrorService.getError(InternalPlatformException.class)).thenReturn(Optional.of(MAPPED_ERROR));
        this.mapper = new InternalPlatformExceptionMapper(bridgeErrorService, TestMappersUtils.getDefaultBuildersMock());
        this.mapper.init();
    }

    @Test
    void testMappedException() {
        ErrorsResponse response = mapper.toResponse(new VaultException("error")).readEntity(ErrorsResponse.class);
        assertThat(response.getItems()).hasSize(1);

        ErrorResponse error = response.getItems().get(0);
        assertThat(error.getId()).isEqualTo("1");
        assertThat(error.getCode()).isEqualTo("mapped-code");
        assertThat(error.getReason()).startsWith("There was an internal exception that is not fixable from the user");
        assertThat(error.getHref()).isEqualTo(TestMappersUtils.getDefaultHref(error.getId()));
    }

}
