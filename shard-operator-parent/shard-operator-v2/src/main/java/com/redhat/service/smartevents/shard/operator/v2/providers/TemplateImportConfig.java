package com.redhat.service.smartevents.shard.operator.v2.providers;

public class TemplateImportConfig {
    private boolean nameToBeSet = false;
    private boolean namespaceToBeSet = false;
    private boolean ownerReferencesToBeSet = false;
    private boolean primaryResourceToBeSet = false;

    public TemplateImportConfig() {
    }

    public TemplateImportConfig(boolean nameToBeSet, boolean namespaceToBeSet, boolean ownerReferencesToBeSet, boolean primaryResourceToBeSet) {
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

    public static TemplateImportConfig withDefaults() {
        return new TemplateImportConfig(true, true, true, false);
    }
}
