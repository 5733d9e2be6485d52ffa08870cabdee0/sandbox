package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.dao.WorkDAO;
import com.redhat.service.bridge.manager.models.ManagedEntity;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;

/*
    A very basic and pretty naive work queue implementation using Postgres and in-memory
    notifications
 */
@Transactional
public class NaivePostgresDbWorkManagerImpl implements WorkManager {

    @Inject
    private WorkDAO workDAO;

    @Inject
    private EventBus eventBus;

    String workerId = UUID.randomUUID().toString();

    /*
        Schedules work into the queue. If there is already work in the queue for the given entity, then we simply
        update the scheduledAt timestamp to let other workers know we're still working on it.
     */
    public Work scheduleWork(ManagedEntity managedEntity) {

        Work w = workDAO.findByEntity(managedEntity);
        if (w == null) {
            w = Work.forResource(managedEntity, this.workerId);
            workDAO.persist(w);
            fireEvent(w);
        } else {
            w.setScheduledAt(ZonedDateTime.now());
        }

        return w;
    }

    private void fireEvent(Work w) {
        eventBus.requestAndForget(w.getType(), w);
    }

    /*
        Finds work for this consumer and schedules it for execution
     */
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void processWorkQueue() {
        workDAO.findByWorkerId(this.workerId).forEach(w -> fireEvent(w));
    }

    /*
        Checks to see if any work item has been stuck in the queue for more than 5 minutes. If yes,
        it assigns the work to itself.
     */
    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void rebalanceWork() {
        ZonedDateTime age = ZonedDateTime.now().minusMinutes(5);
        workDAO.rebalanceWork(this.workerId, age);
    }

    public void completeWork(Work work) {
        workDAO.delete(work);
    }
}
