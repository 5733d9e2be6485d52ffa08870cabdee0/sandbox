package com.redhat.service.smartevents.shard.operator.utils;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentSpecUtils {

    // returns true if the selector, the image and the env variables are the same
    public static boolean isDeploymentEqual(Deployment expected, Deployment existing) {
        return expected.getSpec().getSelector().getMatchLabels().equals(existing.getSpec().getSelector().getMatchLabels())
                && expected.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().equals(existing.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv())
                && expected.getSpec().getTemplate().getSpec().getContainers().get(0).getImage().equals(existing.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
                && expected.getSpec().getProgressDeadlineSeconds().equals(existing.getSpec().getProgressDeadlineSeconds());
    }
}
