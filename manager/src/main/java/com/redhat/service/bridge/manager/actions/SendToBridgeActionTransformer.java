package com.redhat.service.bridge.manager.actions;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Bridge;

@ApplicationScoped
public class SendToBridgeActionTransformer implements ActionTransformer {

    public static final String TYPE = "SendToBridge";
    public static final String TRANSFORMED_TYPE = "Webhook";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public BaseAction transform(Bridge bridge, ProcessorRequest processorRequest) {
        BaseAction action = processorRequest.getAction();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("endpoint", bridge.getEndpoint());

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(TRANSFORMED_TYPE);
        transformedAction.setName(action.getName());
        transformedAction.setParameters(parameters);

        return transformedAction;
    }
}
