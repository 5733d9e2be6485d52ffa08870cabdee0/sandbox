package com.redhat.service.smartevents.shard.operator.core.providers;

import com.redhat.service.smartevents.shard.operator.core.utils.WebClientUtils;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@Dependent
public class WebClientManagerProducer {

    @ConfigProperty(name = "event-bridge.manager.url")
    String eventBridgeManagerUrl;

    @Inject
    Vertx vertx;

    @Produces
    public WebClient produce() {
        return WebClientUtils.getClient(vertx, eventBridgeManagerUrl);
    }
}
