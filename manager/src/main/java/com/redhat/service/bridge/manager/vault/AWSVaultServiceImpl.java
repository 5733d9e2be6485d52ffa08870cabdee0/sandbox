package com.redhat.service.bridge.manager.vault;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.VaultException;
import com.redhat.service.bridge.infra.models.EventBridgeSecret;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;

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

    public static final int MAX_RETRIES = 10;

    @Inject
    SecretsManagerAsyncClient asyncClient;

    @Override
    public Uni<Void> createOrReplace(EventBridgeSecret secret) {
        AtomicInteger counter = new AtomicInteger(0);
        CreateSecretRequest createSecretRequest = CreateSecretRequest
                .builder()
                .name(secret.getId())
                .secretString(Json.encode(secret.getValues()))
                .build();
        return Uni.createFrom().future(asyncClient.createSecret(createSecretRequest))
                .replaceWithVoid()
                .onItem().invoke(() -> LOGGER.info("Secret '{}' created in AWS Vault", secret.getId()))
                .onFailure().retry().until(e -> !(e instanceof ResourceExistsException) && counter.getAndIncrement() <= MAX_RETRIES)
                .onFailure(ResourceExistsException.class).recoverWithUni(() -> replaceSecret(secret))
                .onFailure().transform(e -> new VaultException("Could not replace secret '%s' in AWS Vault", e));
    }

    @Override
    public Uni<EventBridgeSecret> get(String name) {
        AtomicInteger counter = new AtomicInteger(0);
        return Uni.createFrom().future(asyncClient.getSecretValue(
                GetSecretValueRequest.builder()
                        .secretId(name)
                        .build()))
                .onFailure().retry().until(e -> !(e instanceof ResourceNotFoundException) && counter.getAndIncrement() <= MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Secret '%s' not found in AWS Vault", e))
                .onItem().invoke(() -> LOGGER.info("Secret '{}' found in AWS Vault", name))
                .flatMap(x -> Uni.createFrom().item(new EventBridgeSecret().setId(name).setValues(
                        Json.decodeValue(x.secretString(), Map.class))));
    }

    @Override
    public Uni<Void> delete(String name) {
        AtomicInteger counter = new AtomicInteger(0);
        return Uni.createFrom().future(asyncClient.deleteSecret(DeleteSecretRequest.builder().secretId(name).build()))
                .replaceWithVoid()
                .onFailure().retry().until(e -> !(e instanceof ResourceNotFoundException) && counter.getAndIncrement() <= MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Secret '%s' not found in AWS Vault", e))
                .onItem().invoke(() -> LOGGER.info("Secret '{}' deleted from AWS Vault", name));
    }

    private Uni<Void> replaceSecret(EventBridgeSecret secret) {
        LOGGER.info("Secret '{}' already exists in AWS Vault. Replacing..", secret.getId());
        AtomicInteger counter = new AtomicInteger(0);
        PutSecretValueRequest putSecretValueRequest = PutSecretValueRequest.builder()
                .secretId(secret.getId())
                .secretString(Json.encode(secret.getValues()))
                .build();
        return Uni.createFrom().future(asyncClient.putSecretValue(putSecretValueRequest))
                .replaceWithVoid()
                .onFailure().retry().until(e -> counter.getAndIncrement() <= MAX_RETRIES)
                .onFailure().transform(e -> new VaultException("Could not replace secret '%s' in AWS Vault", e))
                .onItem().invoke(() -> LOGGER.info("Secret '{}' replaced", secret.getId()));
    }
}
