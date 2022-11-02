package com.redhat.service.smartevents.shard.operator.resources;

import com.redhat.service.smartevents.shard.operator.utils.NamespaceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

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


    public static final String COMPONENT_NAME = "executor";
    public static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    /**
     * Don't use this default constructor!
     * This class should have a private default constructor. Unfortunately, it's a CR which is created via reflection by fabric8.
     * <p/>
     */
    public BridgeExecutor() {
        this.setStatus(new BridgeExecutorStatus());
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setType(ProcessorType.fromString(this.getSpec().getProcessorType()));
        processorDTO.setId(this.getSpec().getId());
        processorDTO.setBridgeId(this.getSpec().getBridgeId());
        processorDTO.setCustomerId(this.getSpec().getCustomerId());
        processorDTO.setOwner(this.getSpec().getOwner());
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
}
