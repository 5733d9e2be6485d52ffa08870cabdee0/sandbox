package com.redhat.service.smartevents.shard.operator.camel;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.camel.CamelIntegration;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class CamelServiceImpl implements CamelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Override
    public Optional<CamelIntegration> fetchOrCreateCamelIntegration(BridgeExecutor bridgeExecutor, Secret secret) {
        ProcessorDTO processorDTO = bridgeExecutor.toDTO();
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace(processorDTO.getCustomerId());

        Processing processing = processorDTO.getDefinition().getProcessing();
        if (processing != null) {
            LOGGER.info("------ Creating a Camel Integration");

            CamelIntegration expectedIntegrationFromDTO = CamelIntegration.fromDTO(processorDTO, namespace.getMetadata().getName(), processing, secret);

            LOGGER.info("------ integration expected: " + expectedIntegrationFromDTO);

            CamelIntegration existingCamelIntegration = kubernetesClient
                    .resources(CamelIntegration.class)
                    .inNamespace(namespace.getMetadata().getName())
                    .withName(CamelIntegration.resolveResourceName(processorDTO.getId()))
                    .get();

            if (existingCamelIntegration == null || !expectedIntegrationFromDTO.getSpec().equals(existingCamelIntegration.getSpec())) {
                LOGGER.info("------ Integration not found, creating...");

                CamelIntegration createdResource = kubernetesClient
                        .resources(CamelIntegration.class)
                        .inNamespace(namespace.getMetadata().getName())
                        .createOrReplace(expectedIntegrationFromDTO);

                LOGGER.info("------ Created resource: " + createdResource);

                return Optional.of(createdResource);
            }
        }

        return Optional.empty();
    }
}
