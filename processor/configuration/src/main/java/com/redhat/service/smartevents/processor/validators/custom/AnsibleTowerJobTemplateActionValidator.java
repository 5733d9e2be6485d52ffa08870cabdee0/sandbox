package com.redhat.service.smartevents.processor.validators.custom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.validators.AbstractGatewayValidator;

@ApplicationScoped
public class AnsibleTowerJobTemplateActionValidator extends AbstractGatewayValidator implements AnsibleTowerJobTemplateAction, CustomGatewayValidator {

    @Inject
    public AnsibleTowerJobTemplateActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }

    @Override
    public ValidationResult applyAdditionalValidations(Gateway gateway) {
        // reuse webhook action validator logic, since parameter names are the same
        // and this action resolves to WebhookAction
        return WebhookActionValidator.additionalValidations(gateway);
    }
}
