package com.redhat.service.bridge.shard.operator.resources;

import java.util.Objects;

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
        processorDTO.setDefinition(this.getSpec().getDefinition());
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
        private ProcessorDefinition definition;

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

        public BridgeExecutor.Builder withDefinition(final ProcessorDefinition definition) {
            this.definition = definition;
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
            bridgeExecutorSpec.setDefinition(definition);

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
            Objects.requireNonNull(this.definition, "[BridgeExecutor] Definition can't be null");
        }
    }
}
