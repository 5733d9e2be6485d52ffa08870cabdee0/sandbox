package com.redhat.service.bridge.manager.workers.resources;

import java.util.List;

import javax.inject.Inject;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.manager.workers.Worker;
import com.redhat.service.bridge.manager.workers.chain.Chain;
import com.redhat.service.bridge.manager.workers.chain.Link;

public abstract class AbstractChainWorker<T extends ManagedResource> implements Worker<T>, Chain<T> {

    @Inject
    WorkManager workManager;

    @Override
    public boolean handleWork(Work work) {
        Link<?> currentLink;
        boolean chainComplete = true;
        List<Link<?>> links = getLinks(work);

        //Process Chain from tail to head
        for (int i = links.size() - 1; i >= 0; i--) {
            currentLink = links.get(i);
            boolean currentLinkComplete = processLink(work, currentLink);
            chainComplete = chainComplete && currentLinkComplete;
        }

        if (chainComplete) {
            workManager.complete(work);
        }

        return chainComplete;
    }

    private boolean processLink(Work work, Link<?> link) {
        ManagedResource resource = link.getManagedResource();
        // It is not strictly necessary to load the ManagedResource when constructing the chain
        // as the individual Workers also load the applicable ManagedResource when completing their work.
        // However, by creating the correct type of Work (i.e with ManagedResourceId *AND* ManagedResource Type)
        // we are ensuring that when Workers run in parallel the Work will trigger the correct Workers with
        // the VertX EventBus.
        Work delegate = Work.forDependentResource(resource, work);
        return link.getSourceWorker().handleWork(delegate);
    }

}
