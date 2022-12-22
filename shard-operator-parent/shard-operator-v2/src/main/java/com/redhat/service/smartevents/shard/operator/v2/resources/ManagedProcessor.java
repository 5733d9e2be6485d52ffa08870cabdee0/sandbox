package com.redhat.service.smartevents.shard.operator.v2.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.core.utils.StringUtils;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

import static java.util.Objects.requireNonNull;

@Group("com.redhat.service.smartevents")
@Version("v2alpha1")
@ShortNames({ "mproc", "mp" })
public class ManagedProcessor extends CustomResource<ManagedProcessorSpec, ManagedProcessorStatus> implements Namespaced {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String COMPONENT_NAME = "managed-processor";
    public static final String PROCESSOR_NAME_PREFIX = "proc-";

    /**
     * Don't use this default constructor!
     * This class should have a private default constructor. Unfortunately, it's a CR which is created via reflection by fabric8.
     * <p/>
     * Use {@link #fromBuilder()} to create new instances.
     */
    public ManagedProcessor() {
        this.setStatus(new ManagedProcessorStatus());
    }

    /**
     * Standard way of creating a new {@link ManagedProcessor}.
     * This class has a public constructor for integration with Kubernetes libraries only.
     * Please don't use the public constructor to create references of this CR.
     *
     * @return a Builder to help client code to create new instances of the CR.
     */
    public static Builder fromBuilder() {
        return new Builder();
    }

    public static ManagedProcessor fromDTO(ProcessorDTO processorDTO, String namespace) {
        return new Builder()
                .withNamespace(namespace)
                .withProcessorId(processorDTO.getId())
                .withBridgeId(processorDTO.getBridgeId())
                .withCustomerId(processorDTO.getCustomerId())
                .withDefinition(processorDTO.getFlows())
                .withProcessorName(processorDTO.getName())
                .build();
    }

    public static String resolveResourceName(String id) {
        return PROCESSOR_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }

    public static final class Builder {

        private String namespace;
        private String processorId;
        private String bridgeId;
        private String customerId;
        private String processorName;
        private ObjectNode processorDefinition;

        public Builder() {

        }

        public Builder withNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withProcessorId(final String processorId) {
            this.processorId = processorId;
            return this;
        }

        public Builder withBridgeId(final String bridgeId) {
            this.bridgeId = bridgeId;
            return this;
        }

        public Builder withCustomerId(final String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withProcessorName(final String processorName) {
            this.processorName = processorName;
            return this;
        }

        public Builder withDefinition(final ObjectNode processorDefinition) {
            this.processorDefinition = processorDefinition;
            return this;
        }

        public ManagedProcessor build() {
            this.validate();
            ObjectMeta meta = new ObjectMetaBuilder()
                    .withName(resolveResourceName(this.processorId))
                    .withNamespace(namespace)
                    .withLabels(new LabelsBuilder()
                            .withCustomerId(customerId)
                            .withComponent(COMPONENT_NAME)
                            .buildWithDefaults(LabelsBuilder.V2_OPERATOR_NAME))
                    .build();

            ManagedProcessorSpec managedProcessorSpec = new ManagedProcessorSpec();
            managedProcessorSpec.setId(processorId);
            managedProcessorSpec.setBridgeId(bridgeId);
            managedProcessorSpec.setName(processorName);

            managedProcessorSpec.setFlows(new JsonNode[] { processorDefinition });

            ManagedProcessor managedProcessor = new ManagedProcessor();
            managedProcessor.setSpec(managedProcessorSpec);
            managedProcessor.setStatus(new ManagedProcessorStatus());
            managedProcessor.setMetadata(meta);

            return managedProcessor;
        }

        private void validate() {
            requireNonNull(StringUtils.emptyToNull(this.processorId), "[ManagedProcessor] Processor id can't be null");
            requireNonNull(StringUtils.emptyToNull(this.processorName), "[ManagedProcessor] Name can't be null");
            requireNonNull(StringUtils.emptyToNull(this.namespace), "[ManagedProcessor] Namespace can't be null");
            requireNonNull(StringUtils.emptyToNull(this.customerId), "[ManagedProcessor] CustomerId can't be null");
            requireNonNull(StringUtils.emptyToNull(this.bridgeId), "[ManagedProcessor] BridgeId can't be null");
            requireNonNull(this.processorDefinition, "[ManagedProcessor] Definition can't be null");
        }
    }
}
