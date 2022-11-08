package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BridgeErrorHelperTest {

    @Mock
    private BridgeErrorService service;

    private BridgeErrorHelper helper;

    @BeforeEach
    public void setup() {
        this.helper = new BridgeErrorHelper();
        this.helper.bridgeErrorService = service;

        when(service.getError(InternalPlatformException.class)).thenReturn(Optional.of(new BridgeError(1, "code", "reason", BridgeErrorType.PLATFORM)));

        this.helper.setup();
    }

    @Test
    void testGetBridgeErrorInstanceWithKnownException() {
        when(service.getError(NullPointerException.class)).thenReturn(Optional.of(new BridgeError(2, "code2", "reason2", BridgeErrorType.USER)));

        BridgeErrorInstance bei = helper.getBridgeErrorInstance(new NullPointerException());

        assertThat(bei).isNotNull();
        assertThat(bei.getId()).isEqualTo(2);
        assertThat(bei.getCode()).isEqualTo("code2");
        assertThat(bei.getReason()).isEqualTo("reason2");
        assertThat(bei.getType()).isEqualTo(BridgeErrorType.USER);
        assertThat(bei.getUuid()).isNotNull();
    }

    @Test
    void testGetBridgeErrorInstanceWithKnownErrorId() {
        when(service.getError(2)).thenReturn(Optional.of(new BridgeError(2, "code2", "reason2", BridgeErrorType.USER)));

        BridgeErrorInstance bei = helper.getBridgeErrorInstance(2, "UUID");

        assertThat(bei).isNotNull();
        assertThat(bei.getId()).isEqualTo(2);
        assertThat(bei.getCode()).isEqualTo("code2");
        assertThat(bei.getReason()).isEqualTo("reason2");
        assertThat(bei.getType()).isEqualTo(BridgeErrorType.USER);
        assertThat(bei.getUuid()).isEqualTo("UUID");
    }

    @Test
    void testGetBridgeErrorInstanceWithUnknownException() {
        BridgeErrorInstance bei = helper.getBridgeErrorInstance(new NullPointerException());

        assertThat(bei).isNotNull();
        assertThat(bei.getId()).isEqualTo(1);
        assertThat(bei.getCode()).isEqualTo("code");
        assertThat(bei.getReason()).isEqualTo("reason");
        assertThat(bei.getType()).isEqualTo(BridgeErrorType.PLATFORM);
        assertThat(bei.getUuid()).isNotNull();
    }

    @Test
    void testGetBridgeErrorInstanceWithUnknownErrorId() {
        BridgeErrorInstance bei = helper.getBridgeErrorInstance(2, "UUID");

        assertThat(bei).isNotNull();
        assertThat(bei.getId()).isEqualTo(1);
        assertThat(bei.getCode()).isEqualTo("code");
        assertThat(bei.getReason()).isEqualTo("reason");
        assertThat(bei.getType()).isEqualTo(BridgeErrorType.PLATFORM);
        assertThat(bei.getUuid()).isNotNull();
    }

    @Test
    void testMakeUserMessageWhenNoBridgeErrorExists() {
        HasErrorInformation hbei = new HasErrorInformation() {
            @Override
            public Integer getErrorId() {
                return null;
            }

            @Override
            public String getErrorUUID() {
                return null;
            }
        };
        String message = helper.makeUserMessage(hbei);

        assertThat(message).isNull();
    }

    @Test
    void testMakeUserMessageWhenBridgeErrorDoesExist() {
        HasErrorInformation hbei = new HasErrorInformation() {
            @Override
            public Integer getErrorId() {
                return 1;
            }

            @Override
            public String getErrorUUID() {
                return "12345";
            }
        };

        when(service.getError(1)).thenReturn(Optional.of(new BridgeError(1, "code", "reason", BridgeErrorType.USER)));

        String message = helper.makeUserMessage(hbei);

        assertThat(message).isNotNull().contains("[code]").contains("reason").contains("12345");
    }

    @Test
    void testMakeUserMessageCheckTrailingFullStopRemoval() {
        HasErrorInformation hbei = new HasErrorInformation() {
            @Override
            public Integer getErrorId() {
                return 1;
            }

            @Override
            public String getErrorUUID() {
                return "12345";
            }
        };

        when(service.getError(1)).thenReturn(Optional.of(new BridgeError(1, "code", "reason.", BridgeErrorType.USER)));

        String message = helper.makeUserMessage(hbei);

        assertThat(message).isNotNull().doesNotContain("..");
    }

}
