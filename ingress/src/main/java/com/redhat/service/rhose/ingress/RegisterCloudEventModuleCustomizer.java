package com.redhat.service.rhose.ingress;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.jackson.JsonFormat;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class RegisterCloudEventModuleCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }
}
