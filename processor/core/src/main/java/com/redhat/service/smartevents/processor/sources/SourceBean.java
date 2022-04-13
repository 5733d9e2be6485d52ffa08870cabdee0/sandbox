package com.redhat.service.smartevents.processor.sources;

import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.GatewayFamily;

public interface SourceBean extends GatewayBean {
    @Override
    default GatewayFamily getFamily() {
        return GatewayFamily.SOURCE;
    }

    default boolean accept(String sourceType) {
        return accept(GatewayFamily.SOURCE, sourceType);
    }
}
