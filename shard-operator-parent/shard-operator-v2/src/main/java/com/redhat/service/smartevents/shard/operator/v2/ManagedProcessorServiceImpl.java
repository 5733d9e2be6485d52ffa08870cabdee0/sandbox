package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateProviderImplV2;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.fabric8.kubernetes.client.KubernetesClient;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

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

    public CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor, String integrationName) {
        TemplateImportConfig config = TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME);
        CamelIntegration expected = new TemplateProviderImplV2().loadCamelIntegrationTemplate(processor, config);

        expected.getMetadata().setName(integrationName);
        expected.getSpec().setFlows(List.of(processor.getSpec().getFlows()));

        String processorNamespace = processor.getMetadata().getNamespace();
        CamelIntegration integration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(processorNamespace)
                .withName(integrationName)
                .get();

        if (integration == null) {
            LOGGER.info("Create CamelIntegration with name '{}' in namespace '{}' for ManagedProcessor with id '{}'",
                    integrationName, processorNamespace, processor.getMetadata().getName());
            return kubernetesClient
                    .resources(CamelIntegration.class)
                    .inNamespace(processorNamespace)
                    .create(expected);
        }

        return integration;
    }

    @Override
    public void deleteManagedProcessor(ProcessorDTO processorDTO) {
        // TBD
    }
}
