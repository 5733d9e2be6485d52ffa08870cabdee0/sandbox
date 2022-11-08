package com.redhat.service.smartevents.infra.v1.api.models.filters;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperFactory {

    private static ObjectMapper mapper = new ObjectMapper();

    public static ObjectMapper get() {
        return mapper;
    }

    private ObjectMapperFactory() {
    }
}
