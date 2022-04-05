package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class BridgeIngressServiceTest {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    public void testBridgeIngressGetWhenCreated() {
        // Given
        BridgeDTO dto = TestSupport.newRequestedBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        BridgeIngress bridgeIngress = bridgeIngressService.getBridgeIngress(dto);
        assertThat(bridgeIngress).isNotNull();
    }

    @Test
    public void testBridgeIngressGetWhenNotCreated() {
        // Given
        BridgeDTO dto = TestSupport.newRequestedBridgeDTO();

        // Then
        BridgeIngress bridgeIngress = bridgeIngressService.getBridgeIngress(dto);
        assertThat(bridgeIngress).isNull();
    }

    @Test
    public void testBridgeIngressCreation() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);

        // Then
        BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
        assertThat(bridgeIngress).isNotNull();

        Secret secret = fetchBridgeIngressSecret(dto);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(bridgeIngress.getMetadata().getName());
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR).length()).isGreaterThan(0);
    }

    @Test
    public void testBridgeIngressRedeployment() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();
        String patchedCustomerId = TestSupport.CUSTOMER_ID + "-patched";

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);
        dto.setCustomerId(patchedCustomerId);
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);

        // Then
        BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
        assertThat(bridgeIngress).isNotNull();
        assertThat(bridgeIngress.getSpec().getCustomerId()).isEqualTo(patchedCustomerId);

        Secret secret = fetchBridgeIngressSecret(dto);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(bridgeIngress.getMetadata().getName());
    }

    @Test
    public void testBridgeIngressCreationTriggersController() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            // The deployment is deployed by the controller
                            Deployment deployment = fetchBridgeIngressDeployment(dto);
                            assertThat(deployment).isNotNull();
                            assertThat(deployment.getSpec().getProgressDeadlineSeconds()).isEqualTo(60);
                            List<EnvVar> environmentVariables = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.SSO_URL_CONFIG_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.SSO_CLIENT_ID_CONFIG_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_INGRESS_CUSTOMER_ID_CONFIG_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_INGRESS_WEBHOOK_TECHNICAL_ACCOUNT_ID)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);

                        });
    }

    @Test
    public void testFetchOrCreateBridgeIngressDeploymentRedeployment() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();
        String patchedImage = TestSupport.INGRESS_IMAGE + "-patched";

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Wait until deployment is created by the controller.
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            // The deployment is deployed by the controller
                            Deployment deployment = fetchBridgeIngressDeployment(dto);
                            assertThat(deployment).isNotNull();
                        });

        // Patch the deployment and replace
        Deployment deployment = fetchBridgeIngressDeployment(dto);
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(patchedImage);
        kubernetesClient.apps().deployments().inNamespace(deployment.getMetadata().getNamespace()).createOrReplace(deployment);

        // Then
        deployment = bridgeIngressService.fetchOrCreateBridgeIngressDeployment(fetchBridgeIngress(dto), fetchBridgeIngressSecret(dto));
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isEqualTo(TestSupport.INGRESS_IMAGE);
    }

    @Test
    public void testBridgeIngressDeletion() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);
        bridgeIngressService.deleteBridgeIngress(dto);
        waitUntilBridgeIngressDoesntExist(dto);

        // Then
        BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
        assertThat(bridgeIngress).isNull();
    }

    @Test
    @Disabled("Delete loop in BridgeIngressController does not get called. Bug in the SDK? https://issues.redhat.com/browse/MGDOBR-128")
    public void testBridgeIngressDeletionRemovesAllLinkedResource() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);
        bridgeIngressService.deleteBridgeIngress(dto);
        waitUntilBridgeIngressDoesntExist(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Deployment deployment = fetchBridgeIngressDeployment(dto);
                            assertThat(deployment).isNull();
                        });
    }

    private BridgeIngress fetchBridgeIngress(BridgeDTO dto) {
        return kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Deployment fetchBridgeIngressDeployment(BridgeDTO dto) {
        return kubernetesClient.apps().deployments()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Secret fetchBridgeIngressSecret(BridgeDTO dto) {
        return kubernetesClient.secrets()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private void waitUntilBridgeIngressExists(BridgeDTO dto) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> {
                            BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
                            assertThat(bridgeIngress).isNotNull();
                        });
    }

    private void waitUntilBridgeIngressDoesntExist(BridgeDTO dto) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> {
                            BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
                            assertThat(bridgeIngress).isNull();
                        });
    }
}
