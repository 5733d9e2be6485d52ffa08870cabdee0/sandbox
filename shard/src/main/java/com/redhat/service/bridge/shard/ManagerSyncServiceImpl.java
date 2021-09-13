package com.redhat.service.bridge.shard;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.exceptions.DeserializationException;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    @Inject
    ObjectMapper mapper;

    @Inject
    OperatorService operatorService;

    @Inject
    WebClient webClientManager;

    @Scheduled(every = "30s")
    void syncUpdatesFromManager() {
        LOGGER.info("[Shard] Fetching updates from Manager for Bridges and Processors to deploy and delete");
        fetchAndProcessBridgesToDeployOrDelete().subscribe().with(
                success -> LOGGER.info("[shard] has processed all the Bridges to deploy or delete"),
                failure -> LOGGER.warn("[shard] something went wrong during the process of the Bridges to be deployed or deleted"));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.info("[shard] Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return webClientManager.put(APIConstants.SHARD_API_BASE_PATH).sendJson(bridgeDTO);
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyProcessorStatusChange(ProcessorDTO processorDTO) {
        return webClientManager.put(APIConstants.SHARD_API_BASE_PATH + "processors").sendJson(processorDTO);
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployOrDelete() {
        return webClientManager.get(APIConstants.SHARD_API_BASE_PATH).send()
                .onItem().transform(this::getBridges)
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    if (y.getStatus().equals(BridgeStatus.REQUESTED)) { // Bridges to deploy
                                        y.setStatus(BridgeStatus.PROVISIONING);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> operatorService.createBridgeDeployment(y),
                                                failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    if (y.getStatus().equals(BridgeStatus.DELETION_REQUESTED)) { // Bridges to delete
                                        y.setStatus(BridgeStatus.DELETED);
                                        operatorService.deleteBridgeDeployment(y);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> LOGGER.info("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                                failure -> failedToSendUpdateToManager(y, failure));
                                    }
                                    LOGGER.warn("[shard] Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                    return Uni.createFrom().voidItem();
                                }).collect(Collectors.toList())));
    }

    @Override
    public Uni<Object> fetchAndProcessorProcessorsToDeployOrDelete() {
        return webClientManager.get(APIConstants.SHARD_API_BASE_PATH + "processors")
                .send().onItem().transform(this::getProcessors)
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream()
                        .map(y -> {
                            if (BridgeStatus.REQUESTED == y.getStatus()) {
                                y.setStatus(BridgeStatus.PROVISIONING);
                                return notifyProcessorStatusChange(y).subscribe().with(
                                        success -> operatorService.createProcessorDeployment(y),
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
            return mapper.readValue(httpResponse.bodyAsString(), typeReference);
        } catch (JsonProcessingException e) {
            LOGGER.warn("[shard] Failed to deserialize response from Manager", e);
            throw new DeserializationException("Failed to deserialize response from Manager.", e);
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }
}
