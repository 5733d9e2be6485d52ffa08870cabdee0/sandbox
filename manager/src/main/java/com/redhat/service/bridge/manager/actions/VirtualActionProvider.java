package com.redhat.service.bridge.manager.actions;

import com.redhat.service.bridge.actions.ActionProvider;

public interface VirtualActionProvider extends ActionProvider {

    ActionTransformer getTransformer();
}
