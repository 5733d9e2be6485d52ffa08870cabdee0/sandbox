package com.redhat.service.smartevents.shard.operator.providers;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.BridgeIngressServiceImpl;
import com.redhat.service.smartevents.shard.operator.app.Platform;
import com.redhat.service.smartevents.shard.operator.app.PlatformConfigProvider;
import com.redhat.service.smartevents.shard.operator.utils.NetworkingConstants;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class IstioGatewayProviderImpl implements IstioGatewayProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    private String gatewayAddress;

    @ConfigProperty(name = "broker.gateway.ip")
    Optional<String> gatewayIP;

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    PlatformConfigProvider platformConfigProvider;

    void setup(@Observes StartupEvent event) {
        if (Platform.OPENSHIFT.equals(platformConfigProvider.getPlatform())) {
            gatewayAddress = extractOpenshiftGatewayAddress(openShiftClient);
        } else {
            if (gatewayIP.isEmpty()) {
                LOGGER.error("'broker.gateway.ip' config property must be set on k8s platform.");
                Quarkus.asyncExit(1);
            }
            gatewayAddress = extractK8sGatewayAddress(openShiftClient);
        }
        if (gatewayAddress == null) {
            LOGGER.error("Could not retrieve the istio gateway address. Please make sure it was properly deployed.");
            Quarkus.asyncExit(1);
        }
    }

    private String extractOpenshiftGatewayAddress(OpenShiftClient openShiftClient) {
        Route route = openShiftClient.routes().inNamespace("istio-system").withName("knative-eventing-broker-gateway-525eca1d5089dbdc").get();
        if (route.getStatus() != null && "Admitted".equals(route.getStatus().getIngress().get(0).getConditions().get(0).getType())) {
            String endpoint = route.getSpec().getHost();
            return route.getSpec().getTls() != null ? NetworkingConstants.HTTPS_SCHEME + endpoint : NetworkingConstants.HTTP_SCHEME + endpoint;
        }
        return null;
    }

    private String extractK8sGatewayAddress(KubernetesClient kubernetesClient) {
        Service service = kubernetesClient.services().inNamespace("istio-system").withName("istio-ingressgateway").get();
        if (service != null) {
            Optional<ServicePort> first = service.getSpec().getPorts().stream().filter(x -> "http2".equals(x.getName())).findFirst();
            if (first.isPresent() && gatewayIP.isPresent()) {
                return NetworkingConstants.HTTP_SCHEME + gatewayIP.get() + ":" + first.get().getNodePort();
            }
        }
        return null;
    }

    @Override
    public String getIstioGatewayAddress() {
        return gatewayAddress;
    }
}
