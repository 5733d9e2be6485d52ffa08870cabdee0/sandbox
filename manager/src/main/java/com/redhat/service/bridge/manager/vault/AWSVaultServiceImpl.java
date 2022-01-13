package com.redhat.service.bridge.manager.vault;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.secretsmanager.caching.SecretCache;
import com.amazonaws.secretsmanager.caching.SecretCacheConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest;
import com.amazonaws.services.secretsmanager.model.DeleteSecretResult;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.VaultException;
import com.redhat.service.bridge.infra.models.EventBridgeSecret;

import io.vertx.core.json.Json;

@ApplicationScoped
public class AWSVaultServiceImpl implements VaultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSVaultServiceImpl.class);

    @ConfigProperty(name = "event-bridge.secrets-manager.aws.region")
    String region;

    @ConfigProperty(name = "event-bridge.secrets-manager.aws.endpoint-override")
    Optional<String> endpoint;

    @ConfigProperty(name = "event-bridge.secrets-manager.aws.access-key-id")
    String accessKeyId;

    @ConfigProperty(name = "event-bridge.secrets-manager.aws.secret-access-key")
    String secretAccessKey;

    private AWSSecretsManager awsSecretsManager;
    private SecretCache secretCache;

    @PostConstruct
    void init() {
        AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyId, secretAccessKey));
        AWSSecretsManagerClientBuilder clientBuilder = AWSSecretsManagerClientBuilder
                .standard()
                .withCredentials(credentials);
        if (endpoint.isPresent()) {
            clientBuilder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint.get(), region));
        } else {
            clientBuilder.withRegion(region);
        }

        AWSSecretsManager client = clientBuilder.build();
        SecretCacheConfiguration cacheConf = new SecretCacheConfiguration()
                .withMaxCacheSize(SecretCacheConfiguration.DEFAULT_MAX_CACHE_SIZE)
                .withCacheItemTTL(SecretCacheConfiguration.DEFAULT_CACHE_ITEM_TTL)
                .withClient(client);
        this.awsSecretsManager = client;
        this.secretCache = new SecretCache(cacheConf);
    }

    @Override
    public void createOrReplace(EventBridgeSecret secret) {
        try {
            CreateSecretRequest createSecretRequest = new CreateSecretRequest()
                    .withName(secret.getId())
                    .withSecretString(Json.encode(secret.getValues()));
            awsSecretsManager.createSecret(createSecretRequest);

            LOGGER.debug("Secret '{}' created in AWS Vault", secret.getId());
        } catch (ResourceExistsException e) {
            LOGGER.info("Secret '{}' already exists in AWS Vault. Replacing..", secret.getId());
            PutSecretValueRequest putSecretValueRequest = new PutSecretValueRequest()
                    .withSecretId(secret.getId())
                    .withSecretString(Json.encode(secret.getValues()));
            awsSecretsManager.putSecretValue(putSecretValueRequest);
            LOGGER.info("Secret '{}' already exists in AWS Vault. Replaced", secret.getId());
        } catch (InvalidRequestException e) {
            String message = String.format("Unable to create secret '%s' in AWS Vault", secret.getId());
            LOGGER.error(message, e);
            throw new VaultException(message, e);
        } catch (RuntimeException e) {
            String message = String.format("Unexpected exception during creation of secret '%s' in AWS Vault", secret.getId());
            LOGGER.error(message, e);
            throw new VaultException(message, e);
        }
    }

    @Override
    public EventBridgeSecret get(String name) {
        try {
            String secret = secretCache.getSecretString(name);
            LOGGER.debug("Secret {} found in AWS Vault", name);
            return new EventBridgeSecret().setId(name).setValues(
                    Json.decodeValue(secret, Map.class));
        } catch (ResourceNotFoundException e) {
            String message = String.format("Secret '%s' not found in AWS Vault", name);
            LOGGER.warn(message, e);
            throw new VaultException(message, e);
        } catch (RuntimeException e) {
            String message = String.format("Unexpected exception while retrieving the secret '%s' in AWS Vault", name);
            LOGGER.error(message, e);
            throw new VaultException(message, e);
        }
    }

    @Override
    public String delete(String name) {
        try {
            DeleteSecretResult result = awsSecretsManager.deleteSecret(
                    new DeleteSecretRequest()
                            .withSecretId(name));
            LOGGER.debug("Deleted secret {} from AWS Vault", name);
            return result.getName();
        } catch (ResourceNotFoundException e) {
            String message = String.format("Secret '%s' not found in AWS Vault", name);
            LOGGER.warn(message, e);
            throw new VaultException(message, e);
        } catch (RuntimeException e) {
            String message = String.format("Unexpected exception during deletion of secret '%s' in AWS Vault", name);
            LOGGER.error(message, e);
            throw new VaultException(message, e);
        }
    }
}
