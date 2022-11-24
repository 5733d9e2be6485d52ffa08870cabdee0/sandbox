package com.redhat.service.smartevents.shard.operator.v2.providers;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProviderImpl;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

// Avoid using quarkus as it gets too complicated and this needs refactoring
// See https://issues.redhat.com/browse/MGDOBR-1294
public class TemplateProviderImplV2 extends TemplateProviderImpl implements TemplateProviderV2 {

    private static final String TEMPLATES_DIR = "/templates";
    private static final String CAMEL_INTEGRATION_PATH = TEMPLATES_DIR + "/camel-integration.yaml";

    @Override
    public CamelIntegration loadCamelIntegrationTemplate(CustomResource owner, TemplateImportConfig config) {
        CamelIntegration camelIntegration = loadYaml(CamelIntegration.class, CAMEL_INTEGRATION_PATH);
        ObjectMeta metadata = camelIntegration.getMetadata();
        updateMetadata(owner, metadata, config);
        return camelIntegration;
    }
}
