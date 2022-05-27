package com.redhat.service.smartevents.processor.actions;

import java.util.Map;

public interface ActionInvoker {
    void onEvent(String event, Map<String, String> traceHeaders);
}
