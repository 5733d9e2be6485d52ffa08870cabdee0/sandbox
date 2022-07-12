package com.redhat.service.smartevents.manager.workers.quartz;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.workers.Worker;
import com.redhat.service.smartevents.manager.workers.resources.BridgeWorker;
import com.redhat.service.smartevents.manager.workers.resources.ProcessorWorker;

import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkConvertor.convertFromJobData;

// Single Job implementation that basically represents what we have now in the Work table queue.
public class WorkJob implements Job {

    @Inject
    ProcessorWorker processorWorker;

    @Inject
    BridgeWorker bridgeWorker;

    @Override
    public void execute(JobExecutionContext context) {
        Work work = convertFromJobData(context.getMergedJobDataMap());
        Worker<?> worker = findWorker(work);
        worker.handleWork(work);
    }

    // Find the Worker that can handle this item of Work.
    // A bit clunky, but should be fine given that we only have two Workers at the minute.
    private Worker<?> findWorker(Work work) {
        if (Bridge.class.getName().equals(work.getType())) {
            return bridgeWorker;
        } else if (Processor.class.getName().equals(work.getType())) {
            return processorWorker;
        }
        throw new IllegalStateException("Unable to locate worker for resource of type '" + work.getType() + "', with id '" + work.getManagedResourceId() + "'");
    }

}
