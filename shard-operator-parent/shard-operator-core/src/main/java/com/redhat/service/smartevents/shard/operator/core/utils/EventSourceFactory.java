package com.redhat.service.smartevents.shard.operator.core.utils;

import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

import static java.util.Collections.EMPTY_SET;

public class EventSourceFactory {

    public static <T extends HasMetadata> EventSource buildInformerFromOwnerReference(EventSourceContext<?> context, String operatorName, String componentName, Class<T> classToWatch) {
        return new InformerEventSource<>(InformerConfiguration.from(classToWatch, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build(), context);
    }

    public static <T extends HasMetadata> EventSource buildInformerFromPrimaryResource(EventSourceContext<?> context, String operatorName, String componentName, Class<T> classToWatch) {
        return new InformerEventSource<>(InformerConfiguration.from(classToWatch, context)
                .withLabelSelector(buildSelectorLabel(operatorName, componentName))
                .withSecondaryToPrimaryMapper(EventSourceFactory::buildPrimaryResourcesRetriever)
                .build(), context);
    }

    private static Set buildPrimaryResourcesRetriever(HasMetadata resource) {
        String name = resource.getMetadata().getAnnotations().get(LabelsBuilder.PRIMARY_RESOURCE_NAME_LABEL);
        String namespace = resource.getMetadata().getAnnotations().get(LabelsBuilder.PRIMARY_RESOURCE_NAMESPACE_LABEL);
        if (name != null && namespace != null) {
            return Set.of(new ResourceID(name, namespace));
        } else {
            return EMPTY_SET;
        }
    }

    private static String buildSelectorLabel(String operatorName, String componentName) {
        return LabelsBuilder.MANAGED_BY_LABEL + "=" + operatorName + "," + LabelsBuilder.COMPONENT_LABEL + "=" + componentName;
    }
}
