package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorStatusDTO;
import com.redhat.service.smartevents.shard.operator.core.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.core.exceptions.DeserializationException;
import com.redhat.service.smartevents.shard.operator.core.metrics.ManagerRequestStatus;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.utils.WebClientUtils;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

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

    @Override
    public Uni<List<BridgeDTO>> fetchBridgesForDataPlane() {
        return getAuthenticatedRequest(webClientManager.get(V2APIConstants.V2_SHARD_API_BASE_PATH), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_FETCH, failure))
                .onItem().transform(this::getBridges);
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatus(List<BridgeStatusDTO> bridgeStatusDTOs) {
        LOGGER.debug("Notifying manager about the new status of the Bridges");
        return getAuthenticatedRequest(webClientManager.put(V2APIConstants.V2_SHARD_API_BASE_PATH), request -> request.sendJson(bridgeStatusDTOs))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);
    }

    @Override
    public Uni<List<ProcessorDTO>> fetchProcessorsForDataPlane() {
        return getAuthenticatedRequest(webClientManager.get(V2APIConstants.V2_SHARD_API_PROCESSORS_PATH), HttpRequest::send)
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_FETCH, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_FETCH, failure))
                .onItem().transform(this::getProcessors);
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyProcessorStatus(List<ProcessorStatusDTO> processorStatusDTOs) {
        LOGGER.debug("Notifying manager about the new status of the processors");
        return getAuthenticatedRequest(webClientManager.put(V2APIConstants.V2_SHARD_API_PROCESSORS_PATH), request -> request.sendJson(processorStatusDTOs))
                .onItem().invoke(success -> updateManagerRequestMetricsOnSuccess(MetricsOperation.OPERATOR_MANAGER_UPDATE, success))
                .onFailure().invoke(failure -> updateManagerRequestMetricsOnFailure(MetricsOperation.OPERATOR_MANAGER_UPDATE, failure))
                .onFailure().retry().withBackOff(WebClientUtils.DEFAULT_BACKOFF).withJitter(WebClientUtils.DEFAULT_JITTER).atMost(WebClientUtils.MAX_RETRIES);
    }

    private List<BridgeDTO> getBridges(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<>() {
        });
    }

    private List<ProcessorDTO> getProcessors(HttpResponse<Buffer> httpResponse) {
        return deserializeResponseBody(httpResponse, new TypeReference<>() {
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
