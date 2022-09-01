package com.redhat.service.smartevents.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

@Startup
@ApplicationScoped
public class SecretRestartHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRestartHandler.class);

    @ConfigProperty(name = "secret.name")
    String secretName;

    @Inject
    KubernetesClient client;

    private volatile String resourceVersion;
    /*
     * track the uid to detect delete/add (there unfortunately doesn't seem
     * to be a hard guarantee about resourceVersions across uids)
     */
    private volatile String uid;

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void checkSecret() {
        Secret secret = client.secrets().inNamespace(client.getNamespace()).withName(secretName).get();
        if (secret == null) {
            return;
        }
        if (resourceVersion == null) {
            resourceVersion = secret.getMetadata().getResourceVersion();
            uid = secret.getMetadata().getUid();
        } else if (!resourceVersion.equals(secret.getMetadata().getResourceVersion())
                || !uid.equals(secret.getMetadata().getUid())) {
            LOGGER.info(secretName + " changed, requires a restart to pickup new configuration");
            Quarkus.asyncExit();
        }
    }
}
