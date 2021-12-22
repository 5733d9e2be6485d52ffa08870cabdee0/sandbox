package com.redhat.service.bridge.shard.operator;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.exceptions.DeserializationException;
import com.redhat.service.bridge.shard.operator.utils.Constants;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.runtime.Quarkus;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);
    private static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    @Inject
    ObjectMapper mapper;

    @Inject
    WebClient webClientManager;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    OidcClient client;

    Tokens currentTokens;

    @PostConstruct
    public void init() {
        try {
            currentTokens = client.getTokens().await().atMost(SSO_CONNECTION_TIMEOUT);
        } catch (RuntimeException e) {
            LOGGER.error("Fatal error: could not fetch initial authentication token from sso server.");
            Quarkus.asyncExit(1);
        }
    }

    @Scheduled(every = "30s")
    void syncUpdatesFromManager() {
        LOGGER.debug("[Shard] Fetching updates from Manager for Bridges and Processors to deploy and delete");
        fetchAndProcessBridgesToDeployOrDelete().subscribe().with(
                success -> processingComplete(BridgeDTO.class),
                failure -> processingFailed(BridgeDTO.class, failure));

        fetchAndProcessProcessorsToDeployOrDelete().subscribe().with(
                success -> processingComplete(ProcessorDTO.class),
                failure -> processingFailed(ProcessorDTO.class, failure));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.debug("[shard] Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH), request -> request.sendJson(bridgeDTO));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO) {
        return getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH + "processors"), request -> request.sendJson(processorDTO));
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH), HttpRequest::send)
                .onItem().transform(this::getBridges)
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    if (y.getStatus().equals(BridgeStatus.REQUESTED)) { // Bridges to deploy
                                        y.setStatus(BridgeStatus.PROVISIONING);
                                        return notifyBridgeStatusChange(y)
                                                .onFailure().retry().atMost(Constants.MAX_HTTP_RETRY)
                                                .subscribe().with(
                                                        success -> bridgeIngressService.createBridgeIngress(y),
                                                        failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    if (y.getStatus().equals(BridgeStatus.DELETION_REQUESTED)) { // Bridges to delete
                                        y.setStatus(BridgeStatus.DELETED);
                                        bridgeIngressService.deleteBridgeIngress(y);
                                        return notifyBridgeStatusChange(y)
                                                .onFailure().retry().atMost(Constants.MAX_HTTP_RETRY)
                                                .subscribe().with(
                                                        success -> LOGGER.debug("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                                        failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    LOGGER.warn("[shard] Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                    return Uni.createFrom().voidItem();
                                }).collect(Collectors.toList())));
    }

    @Override
    public Uni<Object> fetchAndProcessProcessorsToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH + "processors"), HttpRequest::send)
                .onItem().transform(this::getProcessors)
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream()
                        .map(y -> {
                            if (BridgeStatus.REQUESTED.equals(y.getStatus())) {
                                y.setStatus(BridgeStatus.PROVISIONING);
                                return notifyProcessorStatusChange(y)
                                        .onFailure().retry().atMost(Constants.MAX_HTTP_RETRY)
                                        .subscribe().with(
                                                success -> bridgeExecutorService.createBridgeExecutor(y),
                                                failure -> failedToSendUpdateToManager(y, failure));
                            }
                            if (BridgeStatus.DELETION_REQUESTED.equals(y.getStatus())) { // Processor to delete
                                y.setStatus(BridgeStatus.DELETED);
                                bridgeExecutorService.deleteBridgeExecutor(y);
                                return notifyProcessorStatusChange(y)
                                        .onFailure().retry().atMost(Constants.MAX_HTTP_RETRY)
                                        .subscribe().with(
                                                success -> LOGGER.debug("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                                failure -> failedToSendUpdateToManager(y, failure));
                            }
                            return Uni.createFrom().voidItem();
                        }).collect(Collectors.toList())));
    }

    private List<ProcessorDTO> getProcessors(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<List<ProcessorDTO>>() {
        });
    }

    private List<BridgeDTO> getBridges(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<List<BridgeDTO>>() {
        });
    }

    private <T> List<T> deserializeResponseBody(HttpResponse<Buffer> httpResponse, TypeReference<List<T>> typeReference) {
        try {
            this.validateHttpResponseBeforeParsing(httpResponse);
            return mapper.readValue(httpResponse.bodyAsString(), typeReference);
        } catch (JsonProcessingException e) {
            LOGGER.warn("[shard] Failed to deserialize response from Manager", e);
            throw new DeserializationException("Failed to deserialize response from Manager.", e);
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }

    private void processingFailed(Class<?> entity, Throwable t) {
        LOGGER.error("[shard] Failure processing entities '{}' to be deployed or deleted", entity.getSimpleName(), t);
    }

    private void processingComplete(Class<?> entity) {
        LOGGER.debug("[shard] Successfully processed all entities '{}' to deploy or delete", entity.getSimpleName());
    }

    /**
     * Verifies if the HTTP response is valid (Status 2xx).
     * This avoids the shard-operator spamming error messages in case the manager is out.
     * A more elaborated use case can be added in the future if needed (like validating if it's a JSON, or if the body doesn't have buffer, or is null).
     *
     * @param httpResponse the given response
     * @throws IllegalStateException in case of an invalid HTTP response
     */
    private void validateHttpResponseBeforeParsing(HttpResponse<Buffer> httpResponse) {
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 400) {
            throw new DeserializationException(String.format("Got %d HTTP status code response, skipping deserialization process", httpResponse.statusCode()));
        }
    }

    private Uni<HttpResponse<Buffer>> getAuthenticatedRequest(HttpRequest<Buffer> request, Function<HttpRequest<Buffer>, Uni<HttpResponse<Buffer>>> executor) {
        Tokens tokens = currentTokens;
        if (tokens.isAccessTokenExpired()) {
            LOGGER.debug("Shard authentication token has expired");
            try {
                tokens = client.refreshTokens(tokens.getRefreshToken()).await().atMost(SSO_CONNECTION_TIMEOUT);
            } catch (RuntimeException e) {
                LOGGER.warn("Shard could not fetch a new authentication token from sso server.");
                throw e;
            }
            currentTokens = tokens;
        }
        request.bearerTokenAuthentication(currentTokens.getAccessToken());
        return executor.apply(request);
    }
}
