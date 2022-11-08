package com.redhat.service.smartevents.infra.core.auth;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

public class OidcSchedulerProducer {

    @Produces
    @ApplicationScoped
    public ScheduledExecutorService produceScheduler() {
        return Executors.newScheduledThreadPool(10);
    }
}
