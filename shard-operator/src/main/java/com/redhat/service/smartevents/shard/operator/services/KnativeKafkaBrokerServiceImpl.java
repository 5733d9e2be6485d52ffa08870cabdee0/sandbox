package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

@ApplicationScoped
public class KnativeKafkaBrokerServiceImpl implements KnativeKafkaBrokerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeKafkaBrokerServiceImpl.class);

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public KnativeBroker createKnativeKafkaBroker(BridgeIngress bridgeIngress) {
        KnativeBroker knativeBroker = templateProvider.loadBridgeIngressBrokerTemplate(bridgeIngress, TemplateImportConfig.withDefaults());
        knativeBroker.getSpec().getConfig().setName(bridgeIngress.getMetadata().getName());
        knativeBroker.getSpec().getConfig().setNamespace(bridgeIngress.getMetadata().getNamespace());
        knativeBroker.getMetadata().getAnnotations().replace(GlobalConfigurationsConstants.KNATIVE_BROKER_EXTERNAL_TOPIC_ANNOTATION_NAME,
                bridgeIngress.getSpec().getKafkaConfiguration().getTopic());
        return knativeBroker;
    }

    @Override
    public KnativeBroker fetchKnativeKafkaBroker(BridgeIngress bridgeIngress) {
        return kubernetesClient.resources(KnativeBroker.class)
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }

    @Override
    public String extractBrokerPath(BridgeIngress bridgeIngress) {
        KnativeBroker broker = fetchKnativeKafkaBroker(bridgeIngress);
        if (broker == null || broker.getStatus() == null || broker.getStatus().getAddress().getUrl() == null) {
            return null;
        }
        try {
            return new URL(broker.getStatus().getAddress().getUrl()).getPath();
        } catch (MalformedURLException e) {
            LOGGER.info("Could not extract URL of the broker of BridgeIngress '{}'", broker.getMetadata().getName());
            return null;
        }
    }
}
