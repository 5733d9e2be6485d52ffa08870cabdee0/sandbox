package com.redhat.service.rhose.ingress.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.rhose.infra.exceptions.mappers.ExternalUserExceptionMapper;

@Provider
@ApplicationScoped
public class IngressExternalUserExceptionMapper extends ExternalUserExceptionMapper {
}
