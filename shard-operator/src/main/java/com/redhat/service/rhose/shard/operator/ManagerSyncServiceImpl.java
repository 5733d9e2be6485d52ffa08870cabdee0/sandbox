package com.redhat.service.rhose.shard.operator;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.rhose.infra.api.APIConstants;
import com.redhat.service.rhose.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.rhose.infra.models.dto.BridgeDTO;
import com.redhat.service.rhose.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.rhose.infra.models.dto.ProcessorDTO;
import com.redhat.service.rhose.shard.operator.exceptions.DeserializationException;
import com.redhat.service.rhose.shard.operator.metrics.ManagerRequestStatus;
import com.redhat.service.rhose.shard.operator.metrics.ManagerRequestType;
import com.redhat.service.rhose.shard.operator.metrics.MetricsService;
import com.redhat.service.rhose.shard.operator.utils.WebClientUtils;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    @Inject
    ObjectMapper mapper;

    @Inject
    WebClient webClientManager;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    EventBridgeOidcClient eventBridgeOidcClient;

    @Inject
    MetricsService metricsService;

    @Scheduled(every = "30s")
    void syncUpdatesFromManager() {
        LOGGER.debug("Fetching updates from Manager for Bridges and Processors to deploy and delete");
        fetchAndProcessBridgesToDeployOrDelete().subscribe().with(
                success -> processingComplete(BridgeDTO.class),
                failure -> processingFailed(BridgeDTO.class, failure));

        fetchAndProcessProcessorsToDeployOrDelete().subscribe().with(
                success -> processingComplete(ProcessorDTO.class),
                failure -> processingFailed(ProcessorDTO.class, failure));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.debug("Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH), request -> request.sendJson(bridgeDTO))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(ManagerRequestType.UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(ManagerRequestType.UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO) {
        LOGGER.debug("Notifying manager about the new status of the Processor '{}'", processorDTO.getId());
        return getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH + "processors"), request -> request.sendJson(processorDTO))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(ManagerRequestType.UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(ManagerRequestType.UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(ManagerRequestType.FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(ManagerRequestType.FETCH, failure))
                .onItem().transform(this::getBridges)
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    if (y.getStatus().equals(ManagedResourceStatus.ACCEPTED)) { // Bridges to deploy
                                        y.setStatus(ManagedResourceStatus.PROVISIONING);
                                        return notifyBridgeStatusChange(y)
                                                .subscribe().with(
                                                        success -> {
                                                            LOGGER.debug("Provisioning notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                                            bridgeIngressService.createBridgeIngress(y);
                                                        },
                                                        failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    if (y.getStatus().equals(ManagedResourceStatus.DEPROVISION)) { // Bridges to delete
                                        y.setStatus(ManagedResourceStatus.DELETING);
                                        return notifyBridgeStatusChange(y)
                                                .subscribe().with(
                                                        success -> {
                                                            LOGGER.debug("Deleting notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                                            bridgeIngressService.deleteBridgeIngress(y);
                                                        },
                                                        failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    LOGGER.warn("Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                    return Uni.createFrom().voidItem();
                                }).collect(Collectors.toList())));
    }

    @Override
    public Uni<Object> fetchAndProcessProcessorsToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH + "processors"), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(ManagerRequestType.FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(ManagerRequestType.FETCH, failure))
                .onItem().transform(this::getProcessors)
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream()
                        .map(y -> {
                            if (ManagedResourceStatus.ACCEPTED.equals(y.getStatus())) {
                                y.setStatus(ManagedResourceStatus.PROVISIONING);
                                return notifyProcessorStatusChange(y)
                                        .subscribe().with(
                                                success -> {
                                                    LOGGER.debug("Provisioning notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                                    bridgeExecutorService.createBridgeExecutor(y);
                                                },
                                                failure -> failedToSendUpdateToManager(y, failure));
                            }
                            if (ManagedResourceStatus.DEPROVISION.equals(y.getStatus())) { // Processor to delete
                                y.setStatus(ManagedResourceStatus.DELETING);
                                return notifyProcessorStatusChange(y)
                                        .subscribe().with(
                                                success -> {
                                                    LOGGER.debug("Deleting notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                                    bridgeExecutorService.deleteBridgeExecutor(y);
                                                },
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
        if (!isSuccessfulResponse(httpResponse)) {
            throw new DeserializationException(String.format("Got %d HTTP status code response, skipping deserialization process", httpResponse.statusCode()));
        }
        try {
            return mapper.readValue(httpResponse.bodyAsString(), typeReference);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to deserialize response from Manager", e);
            throw new DeserializationException("Failed to deserialize response from Manager.", e);
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }

    private void processingFailed(Class<?> entity, Throwable t) {
        LOGGER.error("Failure processing entities '{}' to be deployed or deleted", entity.getSimpleName(), t);
    }

    private void processingComplete(Class<?> entity) {
        LOGGER.debug("Successfully processed all entities '{}' to deploy or delete", entity.getSimpleName());
    }

    private Uni<HttpResponse<Buffer>> getAuthenticatedRequest(HttpRequest<Buffer> request, Function<HttpRequest<Buffer>, Uni<HttpResponse<Buffer>>> executor) {
        request.bearerTokenAuthentication(eventBridgeOidcClient.getToken());
        return executor.apply(request)
                .onItem().transformToUni(x -> {
                    if (isSuccessfulResponse(x)) {
                        return Uni.createFrom().item(x);
                    } else {
                        return Uni.createFrom().failure(
                                new HTTPResponseException(String.format("Shard failed to communicate with the manager, the request failed with status %s", x.statusCode()), x.statusCode()));
                    }
                });
    }

    /**
     * Verifies if the HTTP response is valid (Status 2xx).
     * This avoids the shard-operator spamming error messages in case the manager is out.
     *
     * @param httpResponse the given response
     */
    private boolean isSuccessfulResponse(HttpResponse<?> httpResponse) {
        return httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 400;
    }

    private void updateManagerRequestMetricsOnSuccess(ManagerRequestType requestType, HttpResponse<Buffer> successResponse) {
        metricsService.updateManagerRequestMetrics(requestType, ManagerRequestStatus.SUCCESS, String.valueOf(successResponse.statusCode()));
    }

    private void updateManagerRequestMetricsOnFailure(ManagerRequestType requestType, Throwable error) {
        String statusCode = null;
        if (error instanceof HTTPResponseException) {
            statusCode = String.valueOf(((HTTPResponseException) error).getStatusCode());
        }
        metricsService.updateManagerRequestMetrics(requestType, ManagerRequestStatus.FAILURE, String.valueOf(statusCode));
    }
}
