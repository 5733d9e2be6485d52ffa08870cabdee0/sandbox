package com.redhat.service.bridge.shard.operator.resources;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String COMPONENT_NAME = "executor";

    private static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    /**
     * Standard way of creating a new {@link BridgeIngress}.
     * This class has a public constructor for integration with Kubernetes libraries only.
     * Please don't use the public constructor to create references of this CR.
     *
     * @return a Builder to help client code to create new instances of the CR.
     */
    public static BridgeExecutor.Builder fromBuilder() {
        return new Builder();
    }

    public static BridgeExecutor fromDTO(ProcessorDTO processorDTO, String namespace, String executorImage) {
        return new Builder()
                .withNamespace(namespace)
                .withProcessorId(processorDTO.getId())
                .withImageName(executorImage)
                .withbridgeDTO(processorDTO.getBridge())
                .withDefinition(processorDTO.getDefinition())
                .withProcessorName(processorDTO.getName())
                .build();
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setId(this.getSpec().getId());
        // TODO: think about removing bridgeDTO from the processorDTO and keep only bridgeId and customerId!
        processorDTO.setBridge(this.getSpec().getBridgeDTO());
        processorDTO.setName(this.getSpec().getProcessorName());

        if (this.getSpec().getProcessorDefinition() != null) {
            try {
                processorDTO.setDefinition(MAPPER.readValue(this.getSpec().getProcessorDefinition(), ProcessorDefinition.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return processorDTO;
    }

    public static String resolveResourceName(String id) {
        return OB_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }

    public static final class Builder {

        private String namespace;
        private String imageName;
        private String processorId;
        private BridgeDTO bridgeDTO;
        private String processorName;
        private ProcessorDefinition processorDefinition;

        private Builder() {

        }

        public BridgeExecutor.Builder withNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public BridgeExecutor.Builder withImageName(final String imageName) {
            this.imageName = imageName;
            return this;
        }

        public BridgeExecutor.Builder withProcessorId(final String processorId) {
            this.processorId = processorId;
            return this;
        }

        public BridgeExecutor.Builder withbridgeDTO(final BridgeDTO bridgeDTO) {
            this.bridgeDTO = bridgeDTO;
            return this;
        }

        public BridgeExecutor.Builder withProcessorName(final String processorName) {
            this.processorName = processorName;
            return this;
        }

        public BridgeExecutor.Builder withDefinition(final ProcessorDefinition processorDefinition) {
            this.processorDefinition = processorDefinition;
            return this;
        }

        public BridgeExecutor build() {
            this.validate();
            ObjectMeta meta = new ObjectMetaBuilder()
                    .withName(resolveResourceName(this.processorId))
                    .withNamespace(namespace)
                    .withLabels(new LabelsBuilder()
                            .withCustomerId(bridgeDTO.getCustomerId())
                            .withComponent(COMPONENT_NAME)
                            .buildWithDefaults())
                    .build();

            BridgeExecutorSpec bridgeExecutorSpec = new BridgeExecutorSpec();
            bridgeExecutorSpec.setImage(imageName);
            bridgeExecutorSpec.setId(processorId);
            bridgeExecutorSpec.setBridgeDTO(bridgeDTO);
            bridgeExecutorSpec.setProcessorName(processorName);

            try {
                bridgeExecutorSpec.setProcessorDefinition(MAPPER.writeValueAsString(processorDefinition));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(String.format("Invalid Processor Definition for processorId: '%s'", processorId), e);
            }

            BridgeExecutor bridgeExecutor = new BridgeExecutor();
            bridgeExecutor.setSpec(bridgeExecutorSpec);
            bridgeExecutor.setMetadata(meta);

            return bridgeExecutor;
        }

        private void validate() {
            Objects.requireNonNull(Strings.emptyToNull(this.imageName), "[BridgeExecutor] Executor Image Name can't be null");
            Objects.requireNonNull(Strings.emptyToNull(this.processorId), "[BridgeExecutor] Processor id can't be null");
            Objects.requireNonNull(Strings.emptyToNull(this.processorName), "[BridgeExecutor] Name can't be null");
            Objects.requireNonNull(Strings.emptyToNull(this.namespace), "[BridgeExecutor] Namespace can't be null");
            Objects.requireNonNull(this.processorDefinition, "[BridgeExecutor] Definition can't be null");
        }
    }
}
