package com.redhat.service.smartevents.manager.core.api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.core.exceptions.CompositeBridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionBuilder;
import com.redhat.service.smartevents.infra.core.exceptions.mappers.JsonMappingExceptionMapper;

@Provider
@ApplicationScoped
public class ManagerJsonMappingExceptionMapper extends JsonMappingExceptionMapper {

    @Inject
    public ManagerJsonMappingExceptionMapper(CompositeBridgeErrorService bridgeErrorService, ErrorHrefVersionBuilder hrefBuilder) {
        super(bridgeErrorService, hrefBuilder);
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

}
