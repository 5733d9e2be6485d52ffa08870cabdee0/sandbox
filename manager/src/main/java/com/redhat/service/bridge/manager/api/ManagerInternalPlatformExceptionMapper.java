package com.redhat.service.bridge.manager.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.bridge.infra.exceptions.mappers.InternalPlatformExceptionMapper;

@Provider
@ApplicationScoped
public class ManagerInternalPlatformExceptionMapper extends InternalPlatformExceptionMapper {
}
