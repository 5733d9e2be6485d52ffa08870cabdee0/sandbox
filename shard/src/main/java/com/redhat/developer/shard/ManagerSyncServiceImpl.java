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
        fetchAndProcessBridgesToDeployFromManager().subscribe().with(
                success -> LOGGER.info("[shard] has processed all the Bridges to deploy"),
                failure -> LOGGER.warn("[shard] something went wrong during the process of the Bridges to be deployed"));

        fetchAndProcessBridgesToDeleteFromManager().subscribe().with(
                success -> LOGGER.info("[shard] has processed all the Bridges to delete"),
                failure -> LOGGER.warn("[shard] something went wrong during the process of the Bridges to be deleted"));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyBridgeStatusChange(BridgeDTO bridgeDTO) {
        LOGGER.info("[shard] Notifying manager about the new status of the Bridge '{}'", bridgeDTO.getId());
        return webClientManager.put("/api/v1/shard/bridges").sendJson(bridgeDTO);
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeployFromManager() {
        return webClientManager.get("/api/v1/shard/bridges/toDeploy").send()
                .onItem().transform(x -> deserializeBridges(x.bodyAsString()))
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    y.setStatus(BridgeStatus.PROVISIONING);
                                    return notifyBridgeStatusChange(y).subscribe().with(
                                            success -> operatorService.createBridgeDeployment(y),
                                            failure -> LOGGER.warn("[shard] could not notify the manager with the new Bridges status"));
                                }).collect(Collectors.toList())));
    }

    @Override
    public Uni<Object> fetchAndProcessBridgesToDeleteFromManager() {
        return webClientManager.get("/api/v1/shard/bridges/toDelete").send()
                .onItem().transform(x -> deserializeBridges(x.bodyAsString()))
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    y.setStatus(BridgeStatus.DELETED);
                                    return notifyBridgeStatusChange(y).subscribe().with(
                                            success -> operatorService.deleteBridgeDeployment(y),
                                            failure -> LOGGER.warn("[shard] could not notify the manager with the new Bridges status"));
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
