package com.redhat.service.smartevents.manager.vault;

import java.time.Duration;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.VaultException;
import com.redhat.service.smartevents.infra.models.VaultSecret;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.ext.web.impl.LRUCache;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

@ApplicationScoped
public class AWSVaultServiceImpl implements VaultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSVaultServiceImpl.class);
    private static final int LRU_CACHE_SIZE = 1024;
    private static final int MAX_RETRIES = 10;
    private static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(1);
    private static final double DEFAULT_JITTER = 0.2;
    private static final LRUCache<String, VaultSecret> CACHE = new LRUCache<>(LRU_CACHE_SIZE);

    @Inject
    SecretsManagerAsyncClient asyncClient;

    @Override
    public Uni<Void> createOrReplace(VaultSecret secret) {
        CreateSecretRequest createSecretRequest = CreateSecretRequest
                .builder()
                .name(secret.getId())
                .secretString(Json.encode(secret.getValues()))
                .build();
        return Uni.createFrom().future(asyncClient.createSecret(createSecretRequest))
                .replaceWithVoid()
                .onFailure(e -> !(e instanceof ResourceExistsException)).retry().withJitter(DEFAULT_JITTER).withBackOff(DEFAULT_BACKOFF).atMost(MAX_RETRIES)
                .onFailure(ResourceExistsException.class).recoverWithUni(() -> replaceSecret(secret))
                .onFailure().transform(e -> new VaultException("Could not replace secret '%s' in AWS Vault", e))
                .invoke(() -> {
                    CACHE.put(secret.getId(), secret);
                    LOGGER.debug("Secret '{}' created in AWS Vault", secret.getId());
                });
    }

    @Override
    public Uni<VaultSecret> get(String name) {
        if (CACHE.containsKey(name)) {
            LOGGER.debug("Secret '{}' found in the cache.", name);
            return Uni.createFrom().item(CACHE.get(name));
        }
        return Uni.createFrom().future(asyncClient.getSecretValue(
                GetSecretValueRequest.builder()
                        .secretId(name)
                        .build()))
                .onFailure(e -> !(e instanceof ResourceNotFoundException)).retry().withJitter(DEFAULT_JITTER).withBackOff(DEFAULT_BACKOFF).atMost(MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Secret '%s' not found in AWS Vault", e))
                .flatMap(x -> {
                    LOGGER.debug("Secret '{}' found in AWS Vault", name);
                    VaultSecret secret = new VaultSecret()
                            .setId(name)
                            .setValues(Json.decodeValue(x.secretString(), Map.class));
                    CACHE.put(name, secret);
                    return Uni.createFrom().item(secret);
                });
    }

    @Override
    public Uni<Void> delete(String name) {
        return Uni.createFrom().future(asyncClient.deleteSecret(DeleteSecretRequest.builder().forceDeleteWithoutRecovery(true).secretId(name).build()))
                .replaceWithVoid()
                .onFailure(e -> !(e instanceof ResourceNotFoundException)).retry().withJitter(DEFAULT_JITTER).withBackOff(DEFAULT_BACKOFF).atMost(MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Secret '%s' not found in AWS Vault", e))
                .invoke(() -> {
                    if (CACHE.containsKey(name)) {
                        CACHE.remove(name);
                        LOGGER.debug("Secret '{}' deleted from cache.", name);
                    }
                    LOGGER.debug("Secret '{}' deleted from AWS Vault.", name);
                });
    }

    private Uni<Void> replaceSecret(VaultSecret secret) {
        LOGGER.debug("Secret '{}' already exists in AWS Vault. Replacing..", secret.getId());
        PutSecretValueRequest putSecretValueRequest = PutSecretValueRequest.builder()
                .secretId(secret.getId())
                .secretString(Json.encode(secret.getValues()))
                .build();
        return Uni.createFrom().future(asyncClient.putSecretValue(putSecretValueRequest))
                .replaceWithVoid()
                .onFailure().retry().withJitter(DEFAULT_JITTER).withBackOff(DEFAULT_BACKOFF).atMost(MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Could not replace secret '%s' in AWS Vault", e))
                .invoke(() -> LOGGER.debug("Secret '{}' replaced", secret.getId()));
    }
}
