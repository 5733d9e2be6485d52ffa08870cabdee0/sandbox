package com.redhat.service.smartevents.shard.operator.v2.metrics;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl;

import io.micrometer.core.instrument.Tag;

@V2
@ApplicationScoped
public class MetricsServiceImpl extends BaseOperatorMetricsServiceImpl {

    @Override
    protected Tag getVersionTag() {
        return Tag.of(VERSION_TAG, V2.class.getSimpleName());
    }
}
