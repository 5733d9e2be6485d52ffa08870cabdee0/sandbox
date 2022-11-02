package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.comparators.ConfigMapComparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.services.KnativeKafkaBrokerConfigMapService;
import io.fabric8.kubernetes.api.model.ConfigMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class KnativeKafkaBrokerConfigMapReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    KnativeKafkaBrokerConfigMapService knativeKafkaBrokerConfigMapService;

    public void reconcile(BridgeIngress bridgeIngress){

        List<ConfigMap> requestResource = createRequiredResources(bridgeIngress);

        List<ConfigMap> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);

/*
        // Nothing to check for ConfigMap
        ConfigMap configMap = bridgeIngressService.fetchOrCreateBridgeIngressConfigMap(bridgeIngress, secret);
        if (!status.isConditionTypeTrue(BridgeIngressStatus.CONFIG_MAP_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.CONFIG_MAP_AVAILABLE);
        }*/
    }

    private List<ConfigMap> createRequiredResources(BridgeIngress bridgeIngress) {
        ConfigMap requestedKafkaSecret = knativeKafkaBrokerConfigMapService.createKnativeKafkaBrokerConfigMap(bridgeIngress);
        return Collections.singletonList(requestedKafkaSecret);
    }

    private List<ConfigMap> fetchDeployedResources(BridgeIngress bridgeIngress) {
        ConfigMap deployedKafkaSecret = knativeKafkaBrokerConfigMapService.fetchKnativeKafkaBrokerConfigMap(bridgeIngress);
        return Collections.singletonList(deployedKafkaSecret);
    }

    private void processDelta(List<ConfigMap> requestedResources, List<ConfigMap> deployedResources) {
        Comparator<ConfigMap> configMapComparator = new ConfigMapComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(ConfigMap.class, configMapComparator, requestedResources, deployedResources);
    }
}
