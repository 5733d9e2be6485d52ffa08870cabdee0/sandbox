package com.redhat.service.smartevents.manager.v1.workers.quartz;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.Worker;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.workers.resources.BridgeWorker;
import com.redhat.service.smartevents.manager.v1.workers.resources.ProcessorWorker;

import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.convertFromJobData;

/**
 * Single Job implementation that invokes the applicable {@link Worker} based on the {@link JobDataMap} .
 */
public class WorkJob implements Job {

    private static final String BRIDGE_WORKER_CLASSNAME = Bridge.class.getName();
    private static final String LEGACY_BRIDGE_WORKER_CLASSNAME = "com.redhat.service.smartevents.manager.models.Bridge";

    private static final String PROCESSOR_WORKER_CLASSNAME = Processor.class.getName();
    private static final String LEGACY_PROCESSOR_WORKER_CLASSNAME = "com.redhat.service.smartevents.manager.models.Processor";

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
        if (isWorkForBridge(work)) {
            return bridgeWorker;
        } else if (isWorkForProcessor(work)) {
            return processorWorker;
        }
        throw new InternalPlatformException("Unable to locate worker for resource of type '" + work.getType() + "', with id '" + work.getManagedResourceId() + "'");
    }

    private boolean isWorkForBridge(Work work) {
        if (BRIDGE_WORKER_CLASSNAME.equals(work.getType())) {
            return true;
        } else {
            return LEGACY_BRIDGE_WORKER_CLASSNAME.equals(work.getType());
        }
    }

    private boolean isWorkForProcessor(Work work) {
        if (PROCESSOR_WORKER_CLASSNAME.equals(work.getType())) {
            return true;
        } else {
            return LEGACY_PROCESSOR_WORKER_CLASSNAME.equals(work.getType());
        }
    }

}
