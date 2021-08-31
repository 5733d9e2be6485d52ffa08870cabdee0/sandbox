package com.redhat.developer.shard;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatusDTO;
import com.redhat.developer.shard.utils.WebClientUtils;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    @ConfigProperty(name = "event-connect.manager.url")
    String eventConnectManagerUrl;

    @Inject
    Vertx vertx;

    @Inject
    OperatorService operatorService;

    WebClient webClient;

    @PostConstruct
    void init() {
        webClient = WebClientUtils.getClient(vertx, eventConnectManagerUrl);
    }

    @Scheduled(every = "20s")
    void syncConnectors() {
        LOGGER.info("[Shard] wakes up to get connectors to deploy");
        fetchAndCreate().subscribe().with(
                success -> LOGGER.info("[shard] has processed all the connectors"),
                failure -> LOGGER.warn("[shard] something went wrong during the process of the connectors to be deployed"));
    }

    @Override
    public Uni<HttpResponse<Buffer>> notifyConnectorStatusChange(ConnectorDTO connectorDTO) {
        LOGGER.info("[shard] Notifying manager about the new status of the connector " + connectorDTO.getId());
        return webClient.post("/shard/connectors/toDeploy").sendJson(connectorDTO);
    }

    private Uni<Object> fetchAndCreate() {
        return webClient.get("/shard/connectors/toDeploy").send()
                .onItem().transform(x -> deserializeConnectors(x.bodyAsString()))
                .onItem().transformToUni(x -> Uni.createFrom().item(
                        x.stream()
                                .map(y -> {
                                    y.setStatus(ConnectorStatusDTO.PROVISIONING);
                                    return notifyConnectorStatusChange(y).subscribe().with(
                                            success -> operatorService.createConnectorDeployment(y),
                                            failure -> LOGGER.warn("[shard] could not notify the manager with the new status"));
                                }).collect(Collectors.toList())));
    }

    private List<ConnectorDTO> deserializeConnectors(String s) {
        try {
            return new ObjectMapper().readValue(s, new TypeReference<List<ConnectorDTO>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("[shard] failed to deserialize connectors to deploy", e);
        }
        return null;
    }
}
