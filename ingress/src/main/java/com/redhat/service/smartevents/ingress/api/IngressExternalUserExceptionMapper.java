package com.redhat.service.smartevents.ingress.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.exceptions.mappers.ExternalUserExceptionMapper;

@Provider
@ApplicationScoped
public class IngressExternalUserExceptionMapper extends ExternalUserExceptionMapper {
}
