package com.redhat.service.smartevents.shard.operator.providers;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.app.Orchestrator;
import com.redhat.service.smartevents.infra.core.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.shard.operator.BridgeIngressServiceImpl;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@UnlessBuildProfile(value = "test") // For tests we provide a mocked bean and this should not start at all.
public class IstioGatewayProviderImpl implements IstioGatewayProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    private Service gatewayService;

    private Integer gatewayServiceHttp2Port;

    @ConfigProperty(name = "event-bridge.istio.gateway.name")
    Optional<String> name;

    @ConfigProperty(name = "event-bridge.istio.gateway.namespace")
    Optional<String> namespace;

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    void setup(@Observes StartupEvent event) {
        if (name.isEmpty() || namespace.isEmpty()) {
            LOGGER.error(
                    "'event-bridge.istio.gateway.name' and 'event-bridge.istio.gateway.namespace' config property must be set on k8s platform. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            return;
        }

        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            gatewayService = extractOpenshiftGatewayService(openShiftClient);
        } else {
            gatewayService = extractK8sGatewayService(openShiftClient);
        }
        if (gatewayService == null) {
            LOGGER.error(
                    "Could not retrieve the istio gateway service. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            return;
        }
        Optional<ServicePort> http2Port = gatewayService.getSpec().getPorts().stream().filter(x -> "http2".equals(x.getName())).findFirst();
        if (http2Port.isEmpty()) {
            LOGGER.error(
                    "Could not retrieve the http2 port for the istio gateway service. Please make sure it was properly deployed. The application keeps running due to https://issues.redhat.com/browse/MGDOBR-940 but the functionalitis are compromised.");
            return;
        }
        gatewayServiceHttp2Port = http2Port.get().getPort();
    }

    private Service extractOpenshiftGatewayService(OpenShiftClient openShiftClient) {
        return openShiftClient.services().inNamespace(namespace.get()).withName(name.get()).get();
    }

    private Service extractK8sGatewayService(KubernetesClient kubernetesClient) {
        return kubernetesClient.services().inNamespace(namespace.get()).withName(name.get()).get();
    }

    @Override
    public Service getIstioGatewayService() {
        return gatewayService;
    }

    @Override
    public Integer getIstioGatewayServicePort() {
        return gatewayServiceHttp2Port;
    }
}
