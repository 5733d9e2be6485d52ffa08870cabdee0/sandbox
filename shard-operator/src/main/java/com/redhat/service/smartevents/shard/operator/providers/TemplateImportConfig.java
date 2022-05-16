package com.redhat.service.smartevents.shard.operator.providers;

public class TemplateImportConfig {
    private boolean nameToBeSet = false;
    private boolean namespaceToBeSet = false;
    private boolean ownerReferencesToBeSet = false;

    public TemplateImportConfig() {
    }

    public TemplateImportConfig(boolean nameToBeSet, boolean namespaceToBeSet, boolean ownerReferencesToBeSet) {
        this.nameToBeSet = nameToBeSet;
        this.namespaceToBeSet = namespaceToBeSet;
        this.ownerReferencesToBeSet = ownerReferencesToBeSet;
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

    public boolean isNameToBeSet() {
        return nameToBeSet;
    }

    public boolean isNamespaceToBeSet() {
        return namespaceToBeSet;
    }

    public boolean isOwnerReferencesToBeSet() {
        return ownerReferencesToBeSet;
    }

    public static TemplateImportConfig withAll() {
        return new TemplateImportConfig(true, true, true);
    }
}