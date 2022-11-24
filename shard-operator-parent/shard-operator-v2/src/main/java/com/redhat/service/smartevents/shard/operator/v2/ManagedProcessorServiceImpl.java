package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.providers.TemplateProviderImplV2;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ManagedProcessorServiceImpl implements ManagedProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void createManagedProcessor(ProcessorDTO processorDTO, String namespace) {

        ManagedProcessor expected = ManagedProcessor.fromDTO(processorDTO, namespace);

        ManagedProcessor existing = kubernetesClient
                .resources(ManagedProcessor.class)
                .inNamespace(namespace)
                .withName(ManagedProcessor.resolveResourceName(processorDTO.getId()))
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            ManagedProcessor managedProcessor = kubernetesClient
                    .resources(ManagedProcessor.class)
                    .inNamespace(namespace)
                    .createOrReplace(expected);
        } else {
            // notify manager of status change
        }
    }

    @Override
    public CamelIntegration fetchOrCreateCamelIntegration(ManagedProcessor processor, String integrationName) {
        TemplateImportConfig config = new TemplateImportConfig(LabelsBuilder.V2_OPERATOR_NAME);
        // If we inject TemplateProviderV2
        CamelIntegration expected = new TemplateProviderImplV2().loadCamelIntegrationTemplate(processor, config);
        expected.getMetadata().setName(integrationName);

        expected.getSpec().setFlows(List.of(processor.getSpec().getFlows()));

        String namespace = processor.getMetadata().getNamespace();
        CamelIntegration integration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(namespace)
                .withName(integrationName)
                .get();

        if (integration == null) {
            return kubernetesClient
                    .resources(CamelIntegration.class)
                    .inNamespace(namespace)
                    .create(expected);
        }

        return integration;
    }
}
