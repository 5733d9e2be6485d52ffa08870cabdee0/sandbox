package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.manager.dao.WorkDAO;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.workers.id.WorkerId;

import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;

public class WorkManagerImpl implements WorkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkManagerImpl.class);

    @Inject
    WorkDAO workDAO;

    @Inject
    EventBus eventBus;

    @Inject
    WorkerId workerId;

    @Override
    public Work schedule(ManagedResource managedResource) {
        Work w = workDAO.findByManagedResource(managedResource);
        if (w == null) {
            w = Work.forResource(managedResource, this.workerId.value());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Scheduling work for '%s' [%s]",
                        w.getManagedResourceId(),
                        w.getType()));
            }
            persist(w);
            fireEvent(w);
        } else {
            w.setScheduledAt(ZonedDateTime.now());
            persist(w);
        }

        return w;
    }

    @Transactional
    protected void persist(Work work) {
        workDAO.persist(workDAO.getEntityManager().merge(work));
    }

    private void fireEvent(Work w) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Executing work for '%s' [%s]",
                    w.getManagedResourceId(),
                    w.getType()));
        }
        eventBus.requestAndForget(w.getType(), w);
    }

    @Override
    @Transactional
    public boolean exists(Work work) {
        return Objects.nonNull(workDAO.findById(work.getId()));
    }

    @Override
    @Transactional
    public void complete(Work work) {
        if (!exists(work)) {
            return;
        }

        workDAO.deleteById(work.getId());
    }

    @SuppressWarnings("unused")
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    protected void processWorkQueue() {
        //Don't keep a transaction active whilst triggering work
        getWorkQueue().forEach(this::fireEvent);
    }

    @Transactional
    protected List<Work> getWorkQueue() {
        return workDAO.findByWorkerId(workerId.value()).collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    @Transactional
    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void adoptOrphanWorkers() {
        ZonedDateTime age = ZonedDateTime.now().minusMinutes(5);
        workDAO.rebalanceWork(this.workerId.value(), age);
    }

}
