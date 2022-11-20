package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BridgeExecutorClusterIPServiceImpl implements BridgeExecutorClusterIPService {

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Service createBridgeExecutorClusterIPService(BridgeExecutor bridgeExecutor) {
        Service expected = templateProvider.loadBridgeExecutorServiceTemplate(bridgeExecutor, TemplateImportConfig.withDefaults());
        expected.getMetadata().getLabels().put(LabelsBuilder.INSTANCE_LABEL, bridgeExecutor.getMetadata().getName());

        // Specs
        expected.getSpec().setSelector(new LabelsBuilder().withAppInstance(bridgeExecutor.getMetadata().getName()).build());
        return expected;
    }

    @Override
    public Service fetchBridgeExecutorClusterIPService(BridgeExecutor bridgeExecutor) {
        return kubernetesClient
                .services()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .get();
    }
}
