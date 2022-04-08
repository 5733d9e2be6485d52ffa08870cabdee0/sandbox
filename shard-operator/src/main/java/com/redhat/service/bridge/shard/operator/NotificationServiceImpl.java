package com.redhat.service.bridge.shard.operator;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestStatus;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestType;
import com.redhat.service.smartevents.shard.operator.metrics.MetricsService;
import com.redhat.service.smartevents.shard.operator.utils.WebClientUtils;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Inject
    WebClient webClientManager;

    @Inject
    EventBridgeOidcClient eventBridgeOidcClient;

    @Inject
    MetricsService metricsService;

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
    public void updateManagerRequestMetricsOnSuccess(ManagerRequestType requestType, HttpResponse<Buffer> successResponse) {
        metricsService.updateManagerRequestMetrics(requestType, ManagerRequestStatus.SUCCESS, String.valueOf(successResponse.statusCode()));
    }

    @Override
    public void updateManagerRequestMetricsOnFailure(ManagerRequestType requestType, Throwable error) {
        String statusCode = null;
        if (error instanceof HTTPResponseException) {
            statusCode = String.valueOf(((HTTPResponseException) error).getStatusCode());
        }
        metricsService.updateManagerRequestMetrics(requestType, ManagerRequestStatus.FAILURE, String.valueOf(statusCode));
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
