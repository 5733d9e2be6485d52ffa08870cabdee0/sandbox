package com.redhat.service.smartevents.shard.operator.v1.metrics;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl;

import io.micrometer.core.instrument.Tag;

@V1
@ApplicationScoped
public class MetricsServiceImpl extends BaseOperatorMetricsServiceImpl {

    @Override
    protected Tag getVersionTag() {
        return Tag.of(VERSION_TAG, V1.class.getSimpleName());
    }
}
