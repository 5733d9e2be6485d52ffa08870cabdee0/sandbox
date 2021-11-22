package com.redhat.service.bridge.shard.operator.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("be")
public class BridgeExecutor extends CustomResource<BridgeExecutorSpec, BridgeExecutorStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "executor";

    private static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    public static BridgeExecutor fromDTO(ProcessorDTO processorDTO, String namespace, String executorImage) {
        ObjectMeta meta = new ObjectMetaBuilder()
                .withName(buildResourceName(processorDTO.getId()))
                .withNamespace(namespace)
                .withLabels(new LabelsBuilder()
                        .withCustomerId(processorDTO.getBridge().getCustomerId())
                        .withComponent(COMPONENT_NAME)
                        .buildWithDefaults())
                .build();

        BridgeExecutorSpec bridgeExecutorSpec = new BridgeExecutorSpec();
        bridgeExecutorSpec.setImage(executorImage);
        // TODO: think about removing bridgeDTO from the processorDTO and keep only bridgeId and customerId!
        bridgeExecutorSpec.setId(processorDTO.getId());
        bridgeExecutorSpec.setBridgeDTO(processorDTO.getBridge());
        bridgeExecutorSpec.setProcessorName(processorDTO.getName()); // metadata.name is sanitized, could not be used.

        if (processorDTO.getDefinition() != null) {
            try {
                bridgeExecutorSpec.setProcessorDefinition(new ObjectMapper().writeValueAsString(processorDTO.getDefinition()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        BridgeExecutor bridgeExecutor = new BridgeExecutor();
        bridgeExecutor.setSpec(bridgeExecutorSpec);
        bridgeExecutor.setMetadata(meta);

        return bridgeExecutor;
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setId(this.getSpec().getId());
        // TODO: think about removing bridgeDTO from the processorDTO and keep only bridgeId and customerId!
        processorDTO.setBridge(this.getSpec().getBridgeDTO());
        processorDTO.setName(this.getSpec().getProcessorName());

        if (this.getSpec().getProcessorDefinition() != null) {
            try {
                processorDTO.setDefinition(new ObjectMapper().readValue(this.getSpec().getProcessorDefinition(), ProcessorDefinition.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return processorDTO;
    }

    public static String buildResourceName(String id) {
        return OB_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }
}
