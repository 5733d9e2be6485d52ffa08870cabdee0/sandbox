package com.redhat.service.smartevents.manager.v2.workers.quartz;

import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.Worker;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.workers.resources.BridgeWorker;

import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.convertFromJobData;

/**
 * Single Job implementation that invokes the applicable {@link Worker} based on the {@link JobDataMap} .
 */
public class WorkJob implements Job {

    @Inject
    Instance<Worker<?>> workers;

    @Override
    public void execute(JobExecutionContext context) {
        Work work = convertFromJobData(context.getMergedJobDataMap());
        Worker<?> worker = findWorker(work);
        worker.handleWork(work);
    }

    // Find the Worker that can handle this item of Work.
    // A bit clunky, but should be fine given that we only have two Workers at the minute.
    private Worker<?> findWorker(Work work) {
        Optional<Worker<?>> worker = workers.stream().filter(x -> x.accept(work)).findFirst();
        if (worker.isPresent()) {
            return worker.get();
        }
        throw new InternalPlatformException("Unable to locate worker for resource of type '" + work.getType() + "', with id '" + work.getManagedResourceId() + "'");
    }
}
