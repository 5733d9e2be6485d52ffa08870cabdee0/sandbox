package com.redhat.service.smartevents.shard.operator.v2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.Namespace;

@ApplicationScoped
public class ManagedBridgeServiceImpl implements ManagedBridgeService {

    @Inject
    private NamespaceProvider namespaceProvider;

    @Override
    public void createManagedBridgeResources(ManagedBridge managedBridge) {
        Namespace namespace = namespaceProvider.fetchOrCreateNamespace(managedBridge);

        /*
         * Pull in the rest of the logic from BridgeIngressserviceimpl to create the other resources
         */
    }

    @Override
    public void deleteManagedBridgeResources(ManagedBridge managedBridge) {

        /*
         * Pull in the rest of the logic from BridgeIngressServiceImpl to delete the other resources
         */

        namespaceProvider.deleteNamespace(managedBridge);
    }
}

