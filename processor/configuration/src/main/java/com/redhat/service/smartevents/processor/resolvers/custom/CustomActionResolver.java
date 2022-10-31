package com.redhat.service.smartevents.processor.resolvers.custom;

import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.resolvers.ActionResolver;

public interface CustomActionResolver<T> extends GatewayBean,
        ActionResolver {
}
