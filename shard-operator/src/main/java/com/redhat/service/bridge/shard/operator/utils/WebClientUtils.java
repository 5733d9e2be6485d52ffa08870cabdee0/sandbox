package com.redhat.service.bridge.shard.operator.utils;

import java.net.URI;
import java.time.Duration;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public final class WebClientUtils {

    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds

    public static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(1);
    public static final double DEFAULT_JITTER = 0.2;
    public static final int MAX_RETRIES = 10;

    private WebClientUtils() {
    }

    public static WebClient getClient(Vertx vertx, String url) {
        URI uri = URI.create(url);
        boolean sslEnabled = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort();
        if (port == -1) {
            if (sslEnabled) {
                port = 443;
            } else {
                port = 80;
            }
        }

        return WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(port)
                .setSsl(sslEnabled)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setLogActivity(true));
    }
}