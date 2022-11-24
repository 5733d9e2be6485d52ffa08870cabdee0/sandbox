package com.redhat.service.smartevents.shard.operator.v2.providers;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;

import io.fabric8.kubernetes.client.CustomResource;

public interface TemplateProviderV2 {
    CamelIntegration loadCamelIntegrationTemplate(CustomResource owner, TemplateImportConfig config);
}
