package com.redhat.service.smartevents.processor.actions.logger;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoggerActionInvokerBuilder implements ActionInvokerBuilder, LoggerAction {

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        return new LoggerActionInvoker();
    }
}
