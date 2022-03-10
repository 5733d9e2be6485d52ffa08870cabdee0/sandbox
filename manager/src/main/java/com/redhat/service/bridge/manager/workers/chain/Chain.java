package com.redhat.service.bridge.manager.workers.chain;

import java.util.List;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.Worker;

public interface Chain<T extends ManagedResource> extends Worker<T> {

    List<Link<?>> getLinks(Work work);

}
