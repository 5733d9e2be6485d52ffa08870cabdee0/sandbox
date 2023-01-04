package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedProcessorConverter;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ManagedProcessorServiceImpl implements ManagedProcessorService {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @Override
    public void createManagedProcessor(ProcessorDTO processorDTO) {
        String expectedNamespace = namespaceProvider.getNamespaceName(processorDTO.getBridgeId());

        ManagedProcessor expected = ManagedProcessorConverter.fromProcessorDTOToManagedProcessor(processorDTO, expectedNamespace);

        ManagedProcessor existing = kubernetesClient
                .resources(ManagedProcessor.class)
                .inNamespace(expected.getMetadata().getNamespace())
                .withName(expected.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            kubernetesClient
                    .resources(ManagedProcessor.class)
                    .inNamespace(expected.getMetadata().getNamespace())
                    .withName(expected.getMetadata().getName())
                    .createOrReplace(expected);
        }
    }

    @Override
    public void deleteManagedProcessor(ProcessorDTO processorDTO) {
        String expectedNamespace = namespaceProvider.getNamespaceName(processorDTO.getBridgeId());
        ManagedProcessor managedProcessor = ManagedProcessorConverter.fromProcessorDTOToManagedProcessor(processorDTO, expectedNamespace);
        kubernetesClient
                .resources(ManagedProcessor.class)
                .inNamespace(managedProcessor.getMetadata().getNamespace())
                .delete(managedProcessor);
    }

    @Override
    public List<ManagedProcessor> fetchAllManagedProcessors() {
        return kubernetesClient.resources(ManagedProcessor.class).inAnyNamespace().list().getItems();
    }
}
