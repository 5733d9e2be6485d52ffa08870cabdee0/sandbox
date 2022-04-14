package com.redhat.service.smartevents.manager.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.exceptions.mappers.ExternalUserExceptionMapper;

@Provider
@ApplicationScoped
public class ManagerExternalUserExceptionMapper extends ExternalUserExceptionMapper {
}
