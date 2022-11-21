package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.comparators.AuthorizationPolicyComparator;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.exceptions.ReconcilationFailedException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.services.IstioAuthorizationPolicyService;
import com.redhat.service.smartevents.shard.operator.services.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class IstioAuthorizationPolicyReconciler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IstioAuthorizationPolicyReconciler.class);
    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    IstioAuthorizationPolicyService istioAuthorizationPolicyService;

    @Inject
    StatusService statusService;

    public void reconcile(BridgeIngress bridgeIngress, String path){
        try {
            List<AuthorizationPolicy> requestResource = createRequiredResources(bridgeIngress, path);

            List<AuthorizationPolicy> deployedResources = fetchDeployedResources(bridgeIngress);

            processDelta(requestResource, deployedResources);

            statusService.updateStatusForSuccessfulReconciliation(bridgeIngress.getStatus(), BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reconcile Istio Authorization Policy", e);
            throw new ReconcilationFailedException(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE, e);
        }
    }

    private List<AuthorizationPolicy> createRequiredResources(BridgeIngress bridgeIngress, String path) {
        AuthorizationPolicy requestedAuthorizationPolicy = istioAuthorizationPolicyService.createIstioAuthorizationPolicy(bridgeIngress, path);
        return Collections.singletonList(requestedAuthorizationPolicy);
    }

    private List<AuthorizationPolicy> fetchDeployedResources(BridgeIngress bridgeIngress) {
        AuthorizationPolicy deployedAuthorizationPolicy = istioAuthorizationPolicyService.fetchIstioAuthorizationPolicy(bridgeIngress);
        return deployedAuthorizationPolicy == null ? Collections.EMPTY_LIST : Collections.singletonList(deployedAuthorizationPolicy);
    }

    private void processDelta(List<AuthorizationPolicy> requestedResources, List<AuthorizationPolicy> deployedResources) {
        Comparator<AuthorizationPolicy> authorizationPolicyComparator = new AuthorizationPolicyComparator();
        deltaProcessorService.processDelta(AuthorizationPolicy.class, authorizationPolicyComparator, requestedResources, deployedResources);
    }
}
