package com.redhat.service.bridge.shard.operator.utils;

import java.net.URI;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebClientUtils {

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
                .setLogActivity(true));
    }
}