package com.redhat.service.smartevents.processor.resolvers.custom;

import com.redhat.service.smartevents.infra.v1.api.models.gateways.Gateway;
import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.resolvers.GatewayResolver;

public interface CustomGatewayResolver<T extends Gateway> extends GatewayBean, GatewayResolver<T> {
}
