package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedProcessorConverter;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateProviderV2;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.resources.camel.CamelIntegration;

import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ManagedProcessorServiceImpl implements ManagedProcessorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @V2
    @Inject
    TemplateProviderV2 templateProvider;

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

    public CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor) {
        TemplateImportConfig config = TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME);
        CamelIntegration expected = templateProvider.loadCamelIntegrationTemplate(processor, config);

        String processorName = processor.getMetadata().getName();
        String integrationName = processor.getMetadata().getName();

        expected.getSpec().setFlows(List.of(processor.getSpec().getFlows()));

        String processorNamespace = processor.getMetadata().getNamespace();
        CamelIntegration integration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(processorNamespace)
                .withName(integrationName)
                .get();

        if (integration == null || !integration.getSpec().equals(expected.getSpec())) {
            LOGGER.info("Create/Update CamelIntegration with name '{}' in namespace '{}' for ManagedProcessor with id '{}'",
                    integrationName, processorNamespace, processorName);
            return kubernetesClient
                    .resources(CamelIntegration.class)
                    .inNamespace(processorNamespace)
                    .createOrReplace(expected);
        } else {
            return integration;
        }
    }
}
