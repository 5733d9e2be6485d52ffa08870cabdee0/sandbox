package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AclBinding {
    private String principal;
    private AclPermission permission;
    private AclOperation operation;
    private AclPatternType patternType;
    private AclResourceType resourceType;
    private String resourceName;

    public AclBinding() {
    }

    public AclBinding(String principal, AclPermission permission, AclOperation operation, AclPatternType patternType, AclResourceType resourceType, String resourceName) {
        setUserPrincipal(principal);
        this.permission = permission;
        this.operation = operation;
        this.patternType = patternType;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public String getPrincipal() {
        return principal;
    }

    private void setPrincipal(String principal) {
        this.principal = principal;
    }

    public void setAnyPrincipal() {
        setPrincipal("User:*");
    }

    public void setUserPrincipal(String userId) {
        setPrincipal("User:" + userId);
    }

    public AclPermission getPermission() {
        return permission;
    }

    public void setPermission(AclPermission permission) {
        this.permission = permission;
    }

    public AclOperation getOperation() {
        return operation;
    }

    public void setOperation(AclOperation operation) {
        this.operation = operation;
    }

    public AclPatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(AclPatternType patternType) {
        this.patternType = patternType;
    }

    public AclResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(AclResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String toString() {
        return "AclBinding{" +
                "principal='" + principal + '\'' +
                ", permission=" + permission +
                ", operation=" + operation +
                ", patternType=" + patternType +
                ", resourceType=" + resourceType +
                ", resourceName='" + resourceName + '\'' +
                '}';
    }
}
