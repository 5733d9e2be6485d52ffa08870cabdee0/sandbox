package com.redhat.service.bridge.shard.operator.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.redhat.service.bridge.infra.api.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.api.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

import static java.util.Objects.requireNonNull;

@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("be")
public class BridgeExecutor extends CustomResource<BridgeExecutorSpec, BridgeExecutorStatus> implements Namespaced {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String COMPONENT_NAME = "executor";

    private static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    /**
     * Don't use this default constructor!
     * This class should have a private default constructor. Unfortunately, it's a CR which is created via reflection by fabric8.
     * <p/>
     * Use {@link #fromBuilder()} to create new instances.
     */
    public BridgeExecutor() {
        this.setStatus(new BridgeExecutorStatus());
    }

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
                .withBridgeId(processorDTO.getBridgeId())
                .withCustomerId(processorDTO.getCustomerId())
                .withImageName(executorImage)
                .withDefinition(processorDTO.getDefinition())
                .withProcessorName(processorDTO.getName())
                .build();
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setId(this.getSpec().getId());
        // TODO: think about removing bridgeDTO from the processorDTO and keep only bridgeId and customerId!
        processorDTO.setBridgeId(this.getSpec().getBridgeId());
        processorDTO.setCustomerId(this.getSpec().getCustomerId());
        processorDTO.setName(this.getSpec().getProcessorName());

        if (this.getSpec().getProcessorDefinition() != null) {
            try {
                processorDTO.setDefinition(MAPPER.readValue(this.getSpec().getProcessorDefinition(), ProcessorDefinition.class));
            } catch (JsonProcessingException e) {
                LOGGER.error("Could not deserialize Processor Definition while converting BridgeExecutor to ProcessorDTO", e);
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
        private String bridgeId;
        private String customerId;
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

        public BridgeExecutor.Builder withBridgeId(final String bridgeId) {
            this.bridgeId = bridgeId;
            return this;
        }

        public BridgeExecutor.Builder withCustomerId(final String customerId) {
            this.customerId = customerId;
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
                            .withCustomerId(customerId)
                            .withComponent(COMPONENT_NAME)
                            .buildWithDefaults())
                    .build();

            BridgeExecutorSpec bridgeExecutorSpec = new BridgeExecutorSpec();
            bridgeExecutorSpec.setImage(imageName);
            bridgeExecutorSpec.setId(processorId);
            bridgeExecutorSpec.setBridgeId(bridgeId);
            bridgeExecutorSpec.setCustomerId(customerId);
            bridgeExecutorSpec.setProcessorName(processorName);

            try {
                bridgeExecutorSpec.setProcessorDefinition(MAPPER.writeValueAsString(processorDefinition));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(String.format("Invalid Processor Definition for processorId: '%s'", processorId), e);
            }

            BridgeExecutor bridgeExecutor = new BridgeExecutor();
            bridgeExecutor.setSpec(bridgeExecutorSpec);
            bridgeExecutor.setStatus(new BridgeExecutorStatus());
            bridgeExecutor.setMetadata(meta);

            return bridgeExecutor;
        }

        private void validate() {
            requireNonNull(Strings.emptyToNull(this.imageName), "[BridgeExecutor] Executor Image Name can't be null");
            requireNonNull(Strings.emptyToNull(this.processorId), "[BridgeExecutor] Processor id can't be null");
            requireNonNull(Strings.emptyToNull(this.processorName), "[BridgeExecutor] Name can't be null");
            requireNonNull(Strings.emptyToNull(this.namespace), "[BridgeExecutor] Namespace can't be null");
            requireNonNull(Strings.emptyToNull(this.customerId), "[BridgeExecutor] CustomerId can't be null");
            requireNonNull(Strings.emptyToNull(this.bridgeId), "[BridgeExecutor] BridgeId can't be null");
            requireNonNull(this.processorDefinition, "[BridgeExecutor] Definition can't be null");
        }
    }
}
