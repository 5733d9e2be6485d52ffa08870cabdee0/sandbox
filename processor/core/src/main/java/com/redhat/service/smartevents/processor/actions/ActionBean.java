package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.GatewayFamily;

public interface ActionBean extends GatewayBean {
    @Override
    default GatewayFamily getFamily() {
        return GatewayFamily.ACTION;
    }

    default boolean accept(String actionType) {
        return accept(GatewayFamily.ACTION, actionType);
    }
}
