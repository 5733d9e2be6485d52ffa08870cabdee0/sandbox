package com.redhat.service.bridge.manager.api;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

import com.redhat.service.bridge.infra.exceptions.mappers.UserFaultExceptionMapper;

@Provider
@ApplicationScoped
public class ManagerUserFaultExceptionMapper extends UserFaultExceptionMapper {
}
