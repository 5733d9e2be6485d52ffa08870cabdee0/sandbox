package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ConfigMapComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerConfigMapService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerConfigMapReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeKafkaBrokerConfigMapReconciler.class);
    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerConfigMapService knativeKafkaBrokerConfigMapService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeIngress bridgeIngress){

        try {
            List<ConfigMap> requestResource = createRequiredResources(bridgeIngress);

            List<ConfigMap> deployedResources = fetchDeployedResources(bridgeIngress);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.CONFIG_MAP_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Knative Kafka Broker ConfigMap", e);
            statusService.updateStatusForFailedReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.CONFIG_MAP_AVAILABLE, e);
            throw e;
        }
    }

    private List<ConfigMap> createRequiredResources(BridgeIngress bridgeIngress) {
        ConfigMap requestedKafkaSecret = knativeKafkaBrokerConfigMapService.createKnativeKafkaBrokerConfigMap(bridgeIngress);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<ConfigMap> fetchDeployedResources(BridgeIngress bridgeIngress) {
        ConfigMap deployedKafkaSecret = knativeKafkaBrokerConfigMapService.fetchKnativeKafkaBrokerConfigMap(bridgeIngress);
        return deployedKafkaSecret == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<ConfigMap> requestedResources, List<ConfigMap> deployedResources) {
        Comparator<ConfigMap> configMapComparator = new ConfigMapComparator();
        deltaProcessorService.processDelta(ConfigMap.class, configMapComparator, requestedResources, deployedResources);
    }
}
