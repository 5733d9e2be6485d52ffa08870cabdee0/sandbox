package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicySpecRuleWhen;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

@ApplicationScoped
public class IstioAuthorizationPolicyServiceImpl implements IstioAuthorizationPolicyService {

    @Inject
    TemplateProvider templateProvider;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Override
    public AuthorizationPolicy createIstioAuthorizationPolicy(BridgeIngress bridgeIngress) {
        AuthorizationPolicy expected = templateProvider.loadBridgeIngressAuthorizationPolicyTemplate(bridgeIngress,
                new TemplateImportConfig().withNameFromParent()
                        .withPrimaryResourceFromParent());
        /**
         * https://github.com/istio/istio/issues/37221
         * In addition to that, we can not set the owner references as it is not in the same namespace of the bridgeIngress.
         */
        expected.getMetadata().setNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());

        expected.getSpec().setAction("ALLOW");
        expected.getSpec().getRules().forEach(x -> x.getTo().get(0).getOperation().getPaths().set(0, path));

        AuthorizationPolicySpecRuleWhen userAuthPolicy = new AuthorizationPolicySpecRuleWhen("request.auth.claims[account_id]", Collections.singletonList(bridgeIngress.getSpec().getCustomerId()));
        AuthorizationPolicySpecRuleWhen serviceAccountsAuthPolicy = new AuthorizationPolicySpecRuleWhen("request.auth.claims[rh-user-id]",
                Arrays.asList(bridgeIngress.getSpec().getCustomerId(),
                        globalConfigurationsProvider.getSsoWebhookClientAccountId()));

        expected.getSpec().getRules().get(0).setWhen(Collections.singletonList(userAuthPolicy));
        expected.getSpec().getRules().get(1).setWhen(Collections.singletonList(serviceAccountsAuthPolicy));
        return expected;
    }

    @Override
    public AuthorizationPolicy fetchIstioAuthorizationPolicy(BridgeIngress bridgeIngress) {
        return kubernetesClient.resources(AuthorizationPolicy.class)
                .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace()) // https://github.com/istio/istio/issues/37221
                .withName(bridgeIngress.getMetadata().getName())
                .get();
    }
}
