package com.redhat.service.smartevents.shard.operator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.models.dto.*;
import com.redhat.service.smartevents.shard.operator.converters.ConditionConverter;
import com.redhat.service.smartevents.shard.operator.exceptions.DeserializationException;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestStatus;
import com.redhat.service.smartevents.shard.operator.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import com.redhat.service.smartevents.shard.operator.utils.WebClientUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@ApplicationScoped
public class ManagerClientImpl implements ManagerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerClientImpl.class);

    @Inject
    WebClient webClientManager;

    @Inject
    EventBridgeOidcClient eventBridgeOidcClient;

    @Inject
    OperatorMetricsService metricsService;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConditionConverter conditionConverter;

    @Override
    public Uni<List<BridgeDTO>> fetchBridgesToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_FETCH, failure))
                .onItem().transform(this::getBridges);
    }

    private List<BridgeDTO> getBridges(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<>() {
        });
    }

    @Override
    public Uni<List<ProcessorDTO>> fetchProcessorsToDeployOrDelete() {
        return getAuthenticatedRequest(webClientManager.get(APIConstants.SHARD_API_BASE_PATH + "processors"), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_FETCH, failure))
                .onItem().transform(this::getProcessors);
    }

    private List<ProcessorDTO> getProcessors(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<>() {
        });
    }

    @Override
    public void notifyBridgeStatusChange(String bridgeId, Set<Condition> conditions) {
        LOGGER.debug("Notifying manager about the new status of the Bridge '{}'", bridgeId);

        List<ConditionDTO> conditionDTOList = conditionConverter.fromConditionsToConditionDTOs(conditions);
        ManagedBridgeStatusUpdateDTO updateDTO = new ManagedBridgeStatusUpdateDTO(bridgeId, conditionDTOList);

        Uni<HttpResponse<Buffer>> responseUni = getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH), request -> request.sendJson(updateDTO))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);

        responseUni.subscribe().with(
                success -> LOGGER.info("Updating Bridge with id '{}' done", updateDTO.getBridgeId()),
                failure -> LOGGER.error("Updating Bridge with id '{}' FAILED", updateDTO.getBridgeId(), failure));

    }

    @Override
    public void notifyProcessorStatusChange(String processorId, Set<Condition> conditions) {
        LOGGER.debug("Notifying manager about the new status of the Processor '{}'", processorId);
        List<ConditionDTO> conditionDTOList = conditionConverter.fromConditionsToConditionDTOs(conditions);
        ManagedProcessorStatusUpdateDTO updateDTO = new ManagedProcessorStatusUpdateDTO(processorId, conditionDTOList);

        Uni<HttpResponse<Buffer>> responseUni =  getAuthenticatedRequest(webClientManager.put(APIConstants.SHARD_API_BASE_PATH + "processors"), request -> request.sendJson(updateDTO))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);

        responseUni.subscribe().with(
                success -> LOGGER.info("Updating Processor with id '{}' done", processorId),
                failure -> LOGGER.error("Updating Processor with id '{}' FAILED", processorId, failure));
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

    void updateManagerRequestMetricsOnSuccess(MetricsOperation operation, HttpResponse<Buffer> successResponse) {
        metricsService.updateManagerRequestMetrics(operation, ManagerRequestStatus.SUCCESS, String.valueOf(successResponse.statusCode()));
    }

    void updateManagerRequestMetricsOnFailure(MetricsOperation operation, Throwable error) {
        String statusCode = null;
        if (error instanceof HTTPResponseException) {
            statusCode = String.valueOf(((HTTPResponseException) error).getStatusCode());
        }
        metricsService.updateManagerRequestMetrics(operation, ManagerRequestStatus.FAILURE, String.valueOf(statusCode));
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

    private boolean isSuccessfulResponse(HttpResponse<?> httpResponse) {
        return httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 400;
    }
}
