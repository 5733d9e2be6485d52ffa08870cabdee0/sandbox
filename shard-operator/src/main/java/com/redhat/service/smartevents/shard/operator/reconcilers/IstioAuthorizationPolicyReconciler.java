package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.AuthorizationPolicyComparator;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.services.IstioAuthorizationPolicyService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class IstioAuthorizationPolicyReconciler {

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    IstioAuthorizationPolicyService istioAuthorizationPolicyService;

    public void reconcile(BridgeIngress bridgeIngress, String path){

        List<AuthorizationPolicy> requestResource = createRequiredResources(bridgeIngress, path);

        List<AuthorizationPolicy> deployedResources = fetchDeployedResources(bridgeIngress);

        processDelta(requestResource, deployedResources);

        /*
        // Nothing to check for Authorization Policy
        bridgeIngressService.fetchOrCreateBridgeIngressAuthorizationPolicy(bridgeIngress, path);
        if (!status.isConditionTypeTrue(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE)) {
            status.markConditionTrue(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE);
        }
        */
    }

    private List<AuthorizationPolicy> createRequiredResources(BridgeIngress bridgeIngress, String path) {
        AuthorizationPolicy requestedAuthorizationPolicy = istioAuthorizationPolicyService.createIstioAuthorizationPolicy(bridgeIngress, path);
        return Collections.singletonList(requestedAuthorizationPolicy);
    }

    private List<AuthorizationPolicy> fetchDeployedResources(BridgeIngress bridgeIngress) {
        AuthorizationPolicy deployedAuthorizationPolicy = istioAuthorizationPolicyService.fetchIstioAuthorizationPolicy(bridgeIngress);
        return Collections.singletonList(deployedAuthorizationPolicy);
    }

    private void processDelta(List<AuthorizationPolicy> requestedResources, List<AuthorizationPolicy> deployedResources) {
        Comparator<AuthorizationPolicy> authorizationPolicyComparator = new AuthorizationPolicyComparator();
        boolean deltaProcessed = deltaProcessorService.processDelta(AuthorizationPolicy.class, authorizationPolicyComparator, requestedResources, deployedResources);
    }
}
