package com.redhat.service.bridge.manager.actions.sendtobridge;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

@ApplicationScoped
public class SendToBridgeAction implements ActionProvider {

    public static final String TYPE = "SendToBridge";
    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Inject
    SendToBridgeActionValidator validator;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public SendToBridgeActionValidator getParameterValidator() {
        return validator;
    }

    @Override
    public ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction) {
        return null;
    }
}
