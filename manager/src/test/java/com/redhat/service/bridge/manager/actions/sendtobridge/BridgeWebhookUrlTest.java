package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;

import static com.redhat.service.bridge.manager.actions.sendtobridge.SendToBridgeActionTransformer.getBridgeWebhookUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BridgeWebhookUrlTest {

    private static final String TEST_URL_1 = "http://www.example.com/events";
    private static final String TEST_URL_2 = "http://www.example.com/ob-123/events";

    @Test
    void testGetBridgeWebhookUrl() throws MalformedURLException {
        assertThat(getBridgeWebhookUrl("http://www.example.com")).isEqualTo(TEST_URL_1);
        assertThat(getBridgeWebhookUrl("http://www.example.com/")).isEqualTo(TEST_URL_1);

        assertThat(getBridgeWebhookUrl("http://www.example.com/ob-123")).isEqualTo(TEST_URL_2);
        assertThat(getBridgeWebhookUrl("http://www.example.com/ob-123/")).isEqualTo(TEST_URL_2);

        assertThatExceptionOfType(MalformedURLException.class)
                .isThrownBy(() -> getBridgeWebhookUrl("www.example.com"));
        assertThatExceptionOfType(MalformedURLException.class)
                .isThrownBy(() -> getBridgeWebhookUrl("www.example.com/ob-123"));
    }

}
