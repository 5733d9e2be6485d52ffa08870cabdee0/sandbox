package com.redhat.service.smartevents.executor;

public class MetricsConstants {
    private MetricsConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String BRIDGE_ID_TAG = "bridgeId";
    public static final String PROCESSOR_ID_TAG = "processorId";
    public static final String PROCESSOR_PROCESSING_TIME_METRIC_NAME = "executor.entire.processing.evaluation";
    public static final String FILTER_PROCESSING_TIME_METRIC_NAME = "executor.filter.evaluation";
    public static final String ACTION_PROCESSING_TIME_METRIC_NAME = "executor.action.evaluation";
    public static final String TRANSFORMATION_PROCESSING_TIME_METRIC_NAME = "executor.transformation.evaluation";
}