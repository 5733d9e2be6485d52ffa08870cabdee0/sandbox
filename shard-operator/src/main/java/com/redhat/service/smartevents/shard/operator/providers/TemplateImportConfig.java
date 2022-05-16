package com.redhat.service.smartevents.shard.operator.providers;

public class TemplateImportConfig {
    private boolean nameToBeSet = false;
    private boolean namespaceToBeSet = false;
    private boolean ownerReferencesToBeSet = false;

    public TemplateImportConfig() {
    }

    public TemplateImportConfig(boolean nameToBeLinked, boolean withLinkNamespace, boolean ownerReferencesToBeSet) {
        this.nameToBeSet = nameToBeLinked;
        this.namespaceToBeSet = withLinkNamespace;
        this.ownerReferencesToBeSet = ownerReferencesToBeSet;
    }

    public TemplateImportConfig withNameFromParent() {
        this.nameToBeSet = true;
        return this;
    }

    public TemplateImportConfig withNamespaceFromParent() {
        this.nameToBeSet = true;
        return this;
    }

    public TemplateImportConfig withOwnerReferencesFromParent() {
        this.nameToBeSet = true;
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