package com.redhat.service.smartevents.shard.operator.core.providers;

import java.util.Objects;

public class TemplateImportConfig {

    private boolean nameToBeSet = false;
    private boolean namespaceToBeSet = false;
    private boolean ownerReferencesToBeSet = false;
    private boolean primaryResourceToBeSet = false;
    private String operatorName;

    public TemplateImportConfig(String operatorName) {
        Objects.requireNonNull(operatorName, "operatorName cannot be null");
        this.operatorName = operatorName;
    }

    public TemplateImportConfig(boolean nameToBeSet, boolean namespaceToBeSet, boolean ownerReferencesToBeSet, boolean primaryResourceToBeSet, String operatorName) {
        this(operatorName);
        this.nameToBeSet = nameToBeSet;
        this.namespaceToBeSet = namespaceToBeSet;
        this.ownerReferencesToBeSet = ownerReferencesToBeSet;
        this.primaryResourceToBeSet = primaryResourceToBeSet;
    }

    public TemplateImportConfig withNameFromParent() {
        this.nameToBeSet = true;
        return this;
    }

    public TemplateImportConfig withNamespaceFromParent() {
        this.namespaceToBeSet = true;
        return this;
    }

    public TemplateImportConfig withOwnerReferencesFromParent() {
        this.ownerReferencesToBeSet = true;
        return this;
    }

    public TemplateImportConfig withPrimaryResourceFromParent() {
        this.primaryResourceToBeSet = true;
        return this;
    }

    public TemplateImportConfig withOperatorName(String operatorName) {
        this.operatorName = operatorName;
        return this;
    }

    public boolean isNameToBeSet() {
        return nameToBeSet;
    }

    public boolean isNamespaceToBeSet() {
        return namespaceToBeSet;
    }

    public boolean isOwnerReferencesToBeSet() {
        return ownerReferencesToBeSet;
    }

    public boolean isPrimaryResourceToBeSet() {
        return primaryResourceToBeSet;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public static TemplateImportConfig withDefaults(String operatorName) {
        return new TemplateImportConfig(true, true, true, false, operatorName);
    }
}