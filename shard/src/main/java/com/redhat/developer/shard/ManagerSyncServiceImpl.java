package com.redhat.developer.shard;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.shard.exceptions.DeserializationException;

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
    void syncBridges() {
        LOGGER.info("[Shard] wakes up to get Bridges to deploy and delete");
        fetchAndProcessBridgesToDeployOrDeleteFromManager().subscribe().with(
                success -> LOGGER.info("[shard] has processed all the Bridges to deploy or delete"),
                failure -> LOGGER.warn("[shard] something went wrong during the process of the Bridges to be deployed or deleted"));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.info("[shard] Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return webClientManager.put("/api/v1/shard/bridges").sendJson(bridgeDTO);
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployOrDeleteFromManager() {
        return webClientManager.get("/api/v1/shard/bridges").send()
                .onItem().transform(x -> deserializeBridges(x.bodyAsString()))
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    if (y.getStatus().equals(BridgeStatus.REQUESTED)) { // Bridges to deploy
                                        y.setStatus(BridgeStatus.PROVISIONING);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> operatorService.createBridgeDeployment(y),
                                                failure -> LOGGER.warn("[shard] could not notify the manager with the new Bridges status"));
                                    }
                                    if (y.getStatus().equals(BridgeStatus.DELETION_REQUESTED)) { // Bridges to delete
                                        y.setStatus(BridgeStatus.DELETED);
                                        operatorService.deleteBridgeDeployment(y);
                                        return notifyBridgeStatusChange(y).subscribe().with(
                                                success -> LOGGER.info("[shard] Delete notification for Bridge '{}' has been sent to the manager successfully", y.getId()),
                                                failure -> LOGGER.warn("[shard] could not notify the manager with the new Bridges status"));
                                    }
                                    LOGGER.warn("[shard] Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                    return Uni.createFrom().voidItem();
                                }).collect(Collectors.toList())));
    }

    private List<BridgeDTO> deserializeBridges(String s) {
        try {
            return mapper.readValue(s, new TypeReference<List<BridgeDTO>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("[shard] Failed to deserialize Bridges to deploy", e);
            throw new DeserializationException("Failed to deserialize Bridges fetched from the manager.", e);
        }
    }
}
