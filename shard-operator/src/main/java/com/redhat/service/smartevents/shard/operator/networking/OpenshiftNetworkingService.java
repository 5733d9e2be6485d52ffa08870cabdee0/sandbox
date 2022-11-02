package com.redhat.service.smartevents.shard.operator.networking;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.EventSourceFactory;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RoutePortBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import static com.redhat.service.smartevents.shard.operator.networking.OpenshiftRouteSpecMatchesHelper.matches;

public class OpenshiftNetworkingService implements NetworkingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkingService.class);
    public static final String CLUSTER_DOMAIN_RESOURCE_NAME = "cluster";

    private final OpenShiftClient client;

    public OpenshiftNetworkingService(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public EventSource buildInformerEventSource(String component) {
        return EventSourceFactory.buildRoutesInformer(client, component);
    }

    @Override
    public boolean delete(String name, String namespace) {
        try {
            return client.routes().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            LOGGER.debug("Can't delete ingress with name '{}' because it does not exist", name);
            return false;
        }
    }
}
