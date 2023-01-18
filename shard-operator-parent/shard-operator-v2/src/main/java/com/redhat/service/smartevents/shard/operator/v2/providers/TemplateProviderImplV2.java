package com.redhat.service.smartevents.shard.operator.v2.providers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateImportConfig;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProviderImpl;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

@V2
@ApplicationScoped
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
