package com.redhat.service.smartevents.shard.operator.resources;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Strings;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InvalidURLException;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import com.redhat.service.smartevents.shard.operator.utils.NamespaceUtil;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

import static java.util.Objects.requireNonNull;

/**
 * OpenBridge Ingress Custom Resource
 */
@Group("com.redhat.service.bridge")
@Version("v1alpha1")
@ShortNames("bi")
public class BridgeIngress extends CustomResource<BridgeIngressSpec, BridgeIngressStatus> implements Namespaced {

    public static final String COMPONENT_NAME = "ingress";
    public static final String OB_RESOURCE_NAME_PREFIX = "ob-";

    public static String resolveResourceName(String id) {
        return OB_RESOURCE_NAME_PREFIX + KubernetesResourceUtil.sanitizeName(id);
    }
}
