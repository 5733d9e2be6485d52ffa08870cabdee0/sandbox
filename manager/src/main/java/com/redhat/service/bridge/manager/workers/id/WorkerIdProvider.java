package com.redhat.service.bridge.manager.workers.id;

import java.util.UUID;

import javax.enterprise.inject.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerIdProvider {

    private static final String RANDOM_STATIC_WORKER_ID = UUID.randomUUID().toString();

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerIdProvider.class);

    private String findPodNameFromKubernetes() {
        String hostname = System.getenv("HOSTNAME");
        LOGGER.trace("POD id from hostname: {}", hostname);

        if (hostname != null && !hostname.isEmpty() && !"localhost".equals(hostname)) {
            return "worker-" + hostname;
        } else {
            return null;
        }
    }

    @Produces
    public String getWorkerId() {
        String podNameFromKubernetes = findPodNameFromKubernetes();
        if(podNameFromKubernetes == null) {
            return RANDOM_STATIC_WORKER_ID;
        } else {
            return podNameFromKubernetes;
        }
    }
}