package com.redhat.service.smartevents.manager;

import java.util.Set;

import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;

/**
 * A clone of {@see ProcessorRequest} however it has setters for the properties, useful in tests.
 */
public class ProcessorRequestForTests extends ProcessorRequest {

    public ProcessorRequestForTests() {
        super();
    }

    public ProcessorRequestForTests(String name, Action action) {
        super(name, action);
    }

    public ProcessorRequestForTests(String name, Source source) {
        super(name, source);
    }

    public ProcessorRequestForTests(String name, Set<BaseFilter> filters, String transformationTemplate, Action action) {
        super(name, filters, transformationTemplate, action);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFilters(Set<BaseFilter> filters) {
        this.filters = filters;
    }

    public void setTransformationTemplate(String transformationTemplate) {
        this.transformationTemplate = transformationTemplate;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setActions(Set<Action> actions) {
        this.actions = actions;
    }

}
