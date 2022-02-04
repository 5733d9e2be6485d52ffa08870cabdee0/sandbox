package com.redhat.service.bridge.ingress.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.bridge.infra.exceptions.mappers.ExternalUserExceptionMapper;

@Provider
@ApplicationScoped
public class IngressExternalUserExceptionMapper extends ExternalUserExceptionMapper {
}
