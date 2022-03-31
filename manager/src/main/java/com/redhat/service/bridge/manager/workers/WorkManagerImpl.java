package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.manager.dao.WorkDAO;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.id.WorkerIdProvider;

import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class WorkManagerImpl implements WorkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkManagerImpl.class);

    @Inject
    WorkDAO workDAO;

    @Inject
    EventBus eventBus;

    @Inject
    WorkerIdProvider workerIdProvider;

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public Work schedule(ManagedResource managedResource) {
        Work w = workDAO.findByManagedResource(managedResource);
        if (w == null) {
            w = Work.forResource(managedResource, workerIdProvider.getWorkerId());
            workDAO.persist(w);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Scheduling work for '%s' [%s]",
                        w.getManagedResourceId(),
                        w.getType()));
            }
        }

        return w;
    }

    private void fireEvent(Work w) {
        setModifiedAt(w);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Executing work for '%s' [%s]",
                    w.getManagedResourceId(),
                    w.getType()));
        }
        eventBus.requestAndForget(w.getType(), w);
    }

    @Transactional
    protected void setModifiedAt(Work work) {
        work.setModifiedAt(ZonedDateTime.now());
    }

    @Override
    @Transactional
    public boolean exists(Work work) {
        return Objects.nonNull(workDAO.findById(work.getId()));
    }

    @Override
    @Transactional
    public void recordAttempt(Work work) {
        if (!exists(work)) {
            return;
        }
        // Work has been serialised by VertX at this point and has therefore lost all affinity with
        // a JPA session. We therefore need to first retrieve the entity before updating it.
        Work w = workDAO.findById(work.getId());
        w.setModifiedAt(ZonedDateTime.now());
        w.setAttempts(w.getAttempts() + 1);
    }

    @Override
    @Transactional
    public void complete(Work work) {
        if (!exists(work)) {
            return;
        }
        // Work has been serialised by VertX at this point and has therefore lost all affinity with
        // a JPA session. We therefore need to delete it by Id and not the entity itself.
        workDAO.deleteById(work.getId());
    }

    @SuppressWarnings("unused")
    @Scheduled(cron = "{event-bridge.resources.workers.schedule}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    protected void processWorkQueue() {
        processWorkQueue(getWorkQueue());
    }

    @Transactional
    protected List<Work> getWorkQueue() {
        return workDAO.findByWorkerId(workerIdProvider.getWorkerId());
    }

    @Transactional(Transactional.TxType.NEVER)
    protected void processWorkQueue(List<Work> work) {
        work.forEach(this::fireEvent);
    }

    @SuppressWarnings("unused")
    @Transactional
    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void reconnectDroppedWorkers() {
        ZonedDateTime age = ZonedDateTime.now().minusMinutes(5);
        workDAO.reconnectDroppedWorkers(workerIdProvider.getWorkerId(), age);
    }

}
