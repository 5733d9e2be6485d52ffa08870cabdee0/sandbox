package com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpecRuleToOperation {
    private List<String> methods;

    private List<String> paths;

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpecRuleToOperation that = (AuthorizationPolicySpecRuleToOperation) o;
        return Objects.equals(methods, that.methods) && Objects.equals(paths, that.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methods, paths);
    }
}
