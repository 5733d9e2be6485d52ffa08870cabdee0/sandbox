package com.redhat.service.bridge.executor;

import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public interface TemplateFactory {
    String build(BaseFilter filter);
}
