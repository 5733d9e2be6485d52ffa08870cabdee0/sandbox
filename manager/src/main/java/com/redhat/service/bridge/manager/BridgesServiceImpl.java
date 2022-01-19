package com.redhat.service.bridge.manager;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.rhoas.RhoasClient;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.smallrye.mutiny.TimeoutException;

import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG;
import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG_DEFAULT_VALUE;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @ConfigProperty(name = ENABLED_FLAG, defaultValue = ENABLED_FLAG_DEFAULT_VALUE)
    boolean rhoasEnabled;
    @ConfigProperty(name = "rhoas.timeout-seconds")
    int rhoasTimeout;
    @ConfigProperty(name = "rhoas.ops-account.client-id")
    String rhoasOpsAccountClientId;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    RhoasClient rhoasClient;

    @Transactional
    @Override
    public Bridge createBridge(String customerId, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setStatus(BridgeStatus.REQUESTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setCustomerId(customerId);
        bridgeDAO.persist(bridge);

        createTopicAndGrantAccessFor(bridge.getId());

        LOGGER.info("Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());
        return bridge;
    }

    @Transactional
    @Override
    public Bridge getBridge(String id) {
        Bridge b = bridgeDAO.findById(id);
        if (b == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' does not exist", id));
        }
        return b;
    }

    @Transactional
    public Bridge getAvailableBridge(String bridgeId, String customerId) {
        Bridge bridge = getBridge(bridgeId, customerId);
        if (BridgeStatus.AVAILABLE != bridge.getStatus()) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), BridgeStatus.AVAILABLE));
        }
        return bridge;
    }

    private Bridge findByIdAndCustomerId(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", id, customerId));
        }
        return bridge;
    }

    @Transactional
    @Override
    public Bridge getBridge(String id, String customerId) {
        return findByIdAndCustomerId(id, customerId);
    }

    @Transactional
    @Override
    public void deleteBridge(String id, String customerId) {
        Long processorsCount = processorService.getProcessorsCount(id, customerId);
        if (processorsCount > 0) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }

        Bridge bridge = findByIdAndCustomerId(id, customerId);
        bridge.setStatus(BridgeStatus.DELETION_REQUESTED);
        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());

        deleteTopicAndRevokeAccessFor(bridge.getId());
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo) {
        return bridgeDAO.findByCustomerId(customerId, queryInfo);
    }

    @Transactional
    @Override
    public List<Bridge> getBridgesByStatuses(List<BridgeStatus> statuses) {
        return bridgeDAO.findByStatuses(statuses);
    }

    @Transactional
    @Override
    public Bridge updateBridge(BridgeDTO bridgeDTO) {
        Bridge bridge = getBridge(bridgeDTO.getId(), bridgeDTO.getCustomerId());
        bridge.setStatus(bridgeDTO.getStatus());
        bridge.setEndpoint(bridgeDTO.getEndpoint());

        if (bridgeDTO.getStatus().equals(BridgeStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
        }

        // Update metrics
        meterRegistry.counter("manager.bridge.status.change",
                Collections.singletonList(Tag.of("status", bridgeDTO.getStatus().toString()))).increment();

        LOGGER.info("Bridge with id '{}' has been updated for customer '{}'", bridge.getId(), bridge.getCustomerId());
        return bridge;
    }

    @Override
    public BridgeDTO toDTO(Bridge bridge) {
        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridge.getId());
        dto.setName(bridge.getName());
        dto.setEndpoint(bridge.getEndpoint());
        dto.setStatus(bridge.getStatus());
        dto.setCustomerId(bridge.getCustomerId());
        return dto;
    }

    @Override
    public BridgeResponse toResponse(Bridge bridge) {
        BridgeResponse response = new BridgeResponse();
        response.setId(bridge.getId());
        response.setName(bridge.getName());
        response.setEndpoint(bridge.getEndpoint());
        response.setSubmittedAt(bridge.getSubmittedAt());
        response.setPublishedAt(bridge.getPublishedAt());
        response.setStatus(bridge.getStatus());
        response.setHref(APIConstants.USER_API_BASE_PATH + bridge.getId());
        return response;
    }

    private void createTopicAndGrantAccessFor(String bridgeId) {
        if (!rhoasEnabled) {
            return;
        }
        try {
            NewTopicInput newTopicInput = new NewTopicInput()
                    .name(topicNameFor(bridgeId))
                    .settings(new TopicSettings().numPartitions(1));

            rhoasClient.createTopicAndGrantAccess(newTopicInput, rhoasOpsAccountClientId, RhoasTopicAccessType.CONSUMER_AND_PRODUCER)
                    .await().atMost(Duration.ofSeconds(rhoasTimeout));
        } catch (CompletionException e) {
            String msg = "Failed creating topic and granting access for bridge " + bridgeId;
            LOGGER.warn(msg, e);
            throw new InternalPlatformException(msg, e);
        } catch (TimeoutException e) {
            String msg = "Timeout reached while creating topic and granting access for bridge " + bridgeId;
            LOGGER.warn(msg, e);
            throw new InternalPlatformException(msg, e);
        }
    }

    private void deleteTopicAndRevokeAccessFor(String bridgeId) {
        if (!rhoasEnabled) {
            return;
        }
        try {
            rhoasClient.deleteTopicAndRevokeAccess(topicNameFor(bridgeId), rhoasOpsAccountClientId, RhoasTopicAccessType.CONSUMER_AND_PRODUCER)
                    .await().atMost(Duration.ofSeconds(rhoasTimeout));
        } catch (CompletionException e) {
            String msg = "Failed deleting topic and revoking access for bridge " + bridgeId;
            LOGGER.warn(msg, e);
            throw new InternalPlatformException(msg, e);
        } catch (TimeoutException e) {
            String msg = "Timeout deleting topic and revoking access for bridge " + bridgeId;
            LOGGER.warn(msg, e);
            throw new InternalPlatformException(msg, e);
        }
    }

    private String topicNameFor(String bridgeId) {
        return String.format("ob-%s", bridgeId);
    }
}
