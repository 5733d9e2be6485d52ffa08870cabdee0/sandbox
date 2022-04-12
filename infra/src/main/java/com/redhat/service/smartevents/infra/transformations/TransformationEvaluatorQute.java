package com.redhat.service.smartevents.infra.transformations;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

public class TransformationEvaluatorQute implements TransformationEvaluator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Template template;

    public TransformationEvaluatorQute(Engine engine, String template) {
        if (template != null) {
            this.template = engine.parse(template);
        }
    }

    @Override
    public String render(Map<String, Object> data) {
        if (template != null) {
            return template.render(data);
        }
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
