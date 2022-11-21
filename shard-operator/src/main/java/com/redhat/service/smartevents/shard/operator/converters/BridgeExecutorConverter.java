package com.redhat.service.smartevents.shard.operator.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.redhat.service.smartevents.infra.models.dto.KafkaConfigurationDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorSpec;
import com.redhat.service.smartevents.shard.operator.resources.KafkaConfiguration;
import com.redhat.service.smartevents.shard.operator.utils.NamespaceUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class BridgeExecutorConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ConfigProperty(name = "event-bridge.executor.image")
    String executorImage;

    public BridgeExecutor fromProcessorDTOToBridgeExecutor(ProcessorDTO processorDTO) {

        BridgeExecutor bridgeExecutor = new BridgeExecutor();
        String namespace = NamespaceUtil.resolveCustomerNamespace(processorDTO.getCustomerId());
        bridgeExecutor.getMetadata().setName(processorDTO.getName());
        bridgeExecutor.getMetadata().setNamespace(namespace);

        BridgeExecutorSpec bridgeExecutorSpec = bridgeExecutor.getSpec();
        bridgeExecutorSpec.setId(processorDTO.getId());
        bridgeExecutorSpec.setBridgeId(processorDTO.getBridgeId());
        bridgeExecutorSpec.setCustomerId(processorDTO.getCustomerId());
        bridgeExecutorSpec.setImage(executorImage);
        bridgeExecutorSpec.setProcessorType(processorDTO.getType().getValue());
        bridgeExecutorSpec.setProcessorName(processorDTO.getName());
        try {
            bridgeExecutorSpec.setProcessorDefinition(MAPPER.writeValueAsString(processorDTO.getDefinition()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(String.format("Invalid Processor Definition for processorId: '%s'", processorDTO.getId()), e);
        }
        bridgeExecutorSpec.setOwner(processorDTO.getOwner());
        bridgeExecutorSpec.setKafkaConfiguration(fromKafkaConfigurationDTOToKafkaConfiguration(processorDTO.getKafkaConnection()));
        validate(bridgeExecutor);
        return bridgeExecutor;
    }

    private KafkaConfiguration fromKafkaConfigurationDTOToKafkaConfiguration(KafkaConfigurationDTO kafkaConfigurationDTO) {
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setBootstrapServers(kafkaConfigurationDTO.getBootstrapServers());
        kafkaConfiguration.setSecurityProtocol(kafkaConfigurationDTO.getSecurityProtocol());
        kafkaConfiguration.setSaslMechanism(kafkaConfigurationDTO.getSaslMechanism());
        kafkaConfiguration.setTopic(kafkaConfigurationDTO.getTopic());
        kafkaConfiguration.setClientId(kafkaConfigurationDTO.getClientId());
        kafkaConfiguration.setClientSecret(kafkaConfigurationDTO.getClientSecret());
        return kafkaConfiguration;
    }

    private void validate(BridgeExecutor bridgeExecutor) {
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getSpec().getImage()), "[BridgeExecutor] Executor Image Name can't be null");
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getSpec().getId()), "[BridgeExecutor] Processor id can't be null");
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getMetadata().getName()), "[BridgeExecutor] Name can't be null");
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getMetadata().getNamespace()), "[BridgeExecutor] Namespace can't be null");
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getSpec().getCustomerId()), "[BridgeExecutor] CustomerId can't be null");
        requireNonNull(Strings.emptyToNull(bridgeExecutor.getSpec().getBridgeId()), "[BridgeExecutor] BridgeId can't be null");
        requireNonNull(bridgeExecutor.getSpec().getProcessorDefinition(), "[BridgeExecutor] Definition can't be null");
    }
}
