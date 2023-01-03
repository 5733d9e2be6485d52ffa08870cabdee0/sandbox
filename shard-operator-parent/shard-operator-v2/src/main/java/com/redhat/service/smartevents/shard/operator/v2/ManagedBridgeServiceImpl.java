package com.redhat.service.smartevents.shard.operator.v2;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsProvider;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicySpecRuleWhen;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedBridgeConverter;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.TLSSpec;
import com.redhat.service.smartevents.shard.operator.v2.utils.Constants;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ManagedBridgeServiceImpl implements ManagedBridgeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedBridgeServiceImpl.class);

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    GlobalConfigurationsProvider globalConfigurationsProvider;

    @Override
    public void createManagedBridge(BridgeDTO bridgeDTO) {

        String expectedNamespace = namespaceProvider.getNamespaceName(bridgeDTO.getId());

        ManagedBridge expected = ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, expectedNamespace);
        namespaceProvider.fetchOrCreateNamespace(expected);

        ManagedBridge existing = kubernetesClient
                .resources(ManagedBridge.class)
                .inNamespace(expected.getMetadata().getNamespace())
                .withName(expected.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            expected = kubernetesClient
                    .resources(ManagedBridge.class)
                    .inNamespace(expected.getMetadata().getNamespace())
                    .withName(expected.getMetadata().getName())
                    .createOrReplace(expected);

            createOrUpdateBridgeSecret(expected);
        }

        /*
         * TODO - Callback to Control Plane will be added in https://issues.redhat.com/browse/MGDOBR-1267
         */

    }

    private void createOrUpdateBridgeSecret(ManagedBridge managedBridge) {
        Secret expected = templateProvider.loadBridgeIngressSecretTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        KafkaConfigurationSpec kafkaConfiguration = managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration();

        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET,
                Base64.getEncoder().encodeToString(managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration().getBootstrapServers().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getUser().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getPassword().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSecurityProtocol().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getTopic().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET, Base64.getEncoder().encodeToString(kafkaConfiguration.getSaslMechanism().getBytes()));

        TLSSpec tlsSpec = managedBridge.getSpec().getDnsConfiguration().getTls();

        expected.getData().put(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET, Base64.getEncoder().encodeToString(tlsSpec.getCertificate().getBytes()));
        expected.getData().put(GlobalConfigurationsConstants.TLS_KEY_SECRET, Base64.getEncoder().encodeToString(tlsSpec.getKey().getBytes()));

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            kubernetesClient
                    .secrets()
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
            LOGGER.info("Create/Update Secret with name '{}' for ManagedBridge with id '{}' in namespace '{}'.", expected.getMetadata().getName(), managedBridge.getMetadata().getName(),
                    managedBridge.getMetadata().getNamespace());
        }
    }

    @Override
    public void deleteManagedBridge(BridgeDTO bridgeDTO) {

        ManagedBridge mb = ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, null);

        /*
         * Pull in the rest of the logic from BridgeIngressServiceImpl to delete the other resources
         */

        namespaceProvider.deleteNamespace(mb);
    }

    @Override
    public Secret fetchBridgeSecret(ManagedBridge managedBridge) {
        return kubernetesClient
                .secrets()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();
    }

    @Override
    public ConfigMap fetchOrCreateBridgeConfigMap(ManagedBridge managedBridge, Secret secret) {
        ConfigMap expected = templateProvider.loadBridgeIngressConfigMapTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_CONFIGMAP, GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_VALUE_CONFIGMAP); // TODO: move to DTO?
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_BOOTSTRAP_SERVERS_CONFIGMAP,
                new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET))));
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_SECRET_REF_NAME_CONFIGMAP, secret.getMetadata().getName());
        expected.getData().replace(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP,
                new String(Base64.getDecoder().decode(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET))));

        ConfigMap existing = kubernetesClient
                .configMaps()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null || !expected.getData().equals(existing.getData())) {
            LOGGER.info("Create/Update ConfigMap with name '{}' in namespace '{}' for ManagedBridge with id '{}'", expected.getMetadata().getName(), expected.getMetadata().getNamespace(),
                    managedBridge.getMetadata().getName());
            return kubernetesClient
                    .configMaps()
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    // Best practice would be to generate a new name for the configmap and replace its reference
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public KnativeBroker fetchOrCreateKnativeBroker(ManagedBridge managedBridge, ConfigMap configMap) {

        KnativeBroker expected = templateProvider.loadBridgeIngressBrokerTemplate(managedBridge, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));
        expected.getSpec().getConfig().setName(configMap.getMetadata().getName());
        expected.getSpec().getConfig().setNamespace(configMap.getMetadata().getNamespace());
        expected.getMetadata().getAnnotations().replace(GlobalConfigurationsConstants.KNATIVE_BROKER_EXTERNAL_TOPIC_ANNOTATION_NAME,
                configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP));

        KnativeBroker existing = kubernetesClient.resources(KnativeBroker.class)
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null) {
            LOGGER.info("Create KnativeBroker with name '{}' in namespace '{}' for ManagedBridge with id '{}'", expected.getMetadata().getName(), expected.getMetadata().getNamespace(),
                    managedBridge.getMetadata().getName());
            return kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .create(expected);
        }

        if (!expected.getSpec().getConfig().equals(existing.getSpec().getConfig())) {
            // knative broker is immutable. We have to delete the resource -> trigger the reconciler -> recreate.
            kubernetesClient
                    .resources(KnativeBroker.class)
                    .inNamespace(managedBridge.getMetadata().getNamespace())
                    .withName(managedBridge.getMetadata().getName())
                    .delete();
            return null;
        }

        return existing;
    }

    @Override
    public AuthorizationPolicy fetchOrCreateBridgeAuthorizationPolicy(ManagedBridge managedBridge, String path) {
        AuthorizationPolicy expected = templateProvider.loadBridgeIngressAuthorizationPolicyTemplate(managedBridge,
                new TemplateImportConfig(LabelsBuilder.V2_OPERATOR_NAME).withNameFromParent()
                        .withPrimaryResourceFromParent());
        /**
         * https://github.com/istio/istio/issues/37221
         * In addition to that, we can not set the owner references as it is not in the same namespace of the bridgeIngress.
         */
        expected.getMetadata().setNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());

        expected.getSpec().setAction("ALLOW");
        expected.getSpec().getRules().forEach(x -> x.getTo().get(0).getOperation().getPaths().set(0, path));

        AuthorizationPolicySpecRuleWhen userAuthPolicy = new AuthorizationPolicySpecRuleWhen("request.auth.claims[account_id]", Collections.singletonList(managedBridge.getSpec().getCustomerId()));
        AuthorizationPolicySpecRuleWhen serviceAccountsAuthPolicy = new AuthorizationPolicySpecRuleWhen("request.auth.claims[rh-user-id]",
                Arrays.asList(managedBridge.getSpec().getCustomerId(),
                        globalConfigurationsProvider.getSsoWebhookClientAccountId()));

        expected.getSpec().getRules().get(0).setWhen(Collections.singletonList(userAuthPolicy));
        expected.getSpec().getRules().get(1).setWhen(Collections.singletonList(serviceAccountsAuthPolicy));
        expected.getSpec().getSelector().setMatchLabels(Collections.singletonMap(Constants.BRIDGE_INGRESS_AUTHORIZATION_POLICY_SELECTOR_LABEL,
                istioGatewayProvider.getIstioGatewayService().getMetadata().getLabels().get(Constants.BRIDGE_INGRESS_AUTHORIZATION_POLICY_SELECTOR_LABEL)));

        AuthorizationPolicy existing = kubernetesClient.resources(AuthorizationPolicy.class)
                .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace()) // https://github.com/istio/istio/issues/37221
                .withName(managedBridge.getMetadata().getName())
                .get();

        if (existing == null || !expected.getSpec().equals(existing.getSpec())) {
            LOGGER.info("Create/Update AuthorizationPolicy with name '{}' in namespace '{}' for ManagedBridge with id '{}'", expected.getMetadata().getName(), expected.getMetadata().getNamespace(),
                    managedBridge.getMetadata().getName());
            return kubernetesClient
                    .resources(AuthorizationPolicy.class)
                    .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace()) // https://github.com/istio/istio/issues/37221
                    .withName(managedBridge.getMetadata().getName())
                    .createOrReplace(expected);
        }

        return existing;
    }

    @Override
    public boolean compareBridgeStatus(ManagedBridge oldBridge, ManagedBridge newBridge) {
        Map<String, Condition> oldBridgeConditionMap = oldBridge.getStatus().getConditions().stream().collect(Collectors.toMap(Condition::getType, condition -> condition));
        Map<String, Condition> newBridgeConditionMap = newBridge.getStatus().getConditions().stream().collect(Collectors.toMap(Condition::getType, condition -> condition));
        for (Map.Entry<String, Condition> oldBridgeConditionEntry : oldBridgeConditionMap.entrySet()) {
            Condition newCondition = newBridgeConditionMap.get(oldBridgeConditionEntry.getKey());
            if (!compareCondition(oldBridgeConditionEntry.getValue(), newCondition)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedBridge fetchManagedBridge(String name, String namespace) {
        return kubernetesClient.resources(ManagedBridge.class).inNamespace(namespace).withName(name).get();
    }

    /**
     * Compare condition on bases of condition equality.
     * 
     * @param oldCondition Old condition
     * @param newCondition New Condition.
     * @return { {@code @True} } if both conditions are equal else { {@code @False} }
     */
    private boolean compareCondition(Condition oldCondition, Condition newCondition) {
        return oldCondition.getStatus().equals(newCondition.getStatus());
    }

    @Override
    public List<ManagedBridge> fetchAllManagedBridges() {
        return kubernetesClient.resources(ManagedBridge.class).inAnyNamespace().list().getItems();
    }
}
