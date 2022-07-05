package com.redhat.service.smartevents.manager.workers;

import org.quartz.JobExecutionContext;

import com.redhat.service.smartevents.manager.models.ManagedResource;

public interface Worker<T extends ManagedResource> {

    T handleWork(JobExecutionContext context);

}