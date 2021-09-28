package com.redhat.service.bridge.executor.transformations;

import io.quarkus.qute.Engine;

public class TransformationEvaluatorFactoryQute implements TransformationEvaluatorFactory {

    private static final Engine engine = Engine.builder().addDefaults().build();

    @Override
    public TransformationEvaluator build(String template) {
        return new TransformationEvaluatorQute(engine, template);
    }
}
