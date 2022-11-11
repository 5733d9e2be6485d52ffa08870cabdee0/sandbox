package com.redhat.service.smartevents.shard.operator.cucumber.steps;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.redhat.service.smartevents.shard.operator.cucumber.common.Context;
import com.redhat.service.smartevents.shard.operator.cucumber.common.TimeUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;

/**
 * Step definitions related to Kubernetes
 */
public class KubernetesSteps {

    private Context context;

    public KubernetesSteps(Context context) {
        this.context = context;
    }

    @Given("^create Namespace$")
    public void createNamespace() {
        context.getClient().namespaces().createOrReplace(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(context.getNamespace())
                        .endMetadata()
                        .build());
    }

    @Then("^the Deployment \"([^\"]*)\" is ready within (\\d+) (?:minute|minutes)$")
    public void theDeploymentIsInStateWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    Deployment deployment = context.getClient().apps().deployments().inNamespace(context.getNamespace()).withName(name).get();
                    if (deployment == null) {
                        return false;
                    }
                    return deployment.getStatus().getConditions().stream().anyMatch(d -> d.getType().equals("Available") && d.getStatus().equals("True"));
                },
                String.format("Timeout waiting for Deployment '%s' to be ready in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^no Deployment exists within (\\d+) (?:minute|minutes)$")
    public void noDeploymentExistsWithinMinutes(int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    DeploymentList deployments = context.getClient().apps().deployments().inNamespace(context.getNamespace()).list();
                    return deployments.getItems().isEmpty();
                },
                String.format("Timeout waiting for all Deployments to be removed", context.getNamespace()));
    }

    @Then("^the Service \"([^\"]*)\" exists within (\\d+) (?:minute|minutes)$")
    public void theServiceExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    Service service = context.getClient().services().inNamespace(context.getNamespace()).withName(name).get();
                    return service != null;
                },
                String.format("Timeout waiting for Service '%s' to exist in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^no Service exists within (\\d+) (?:minute|minutes)$")
    public void noServiceExistsWithinMinutes(int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    ServiceList services = context.getClient().services().inNamespace(context.getNamespace()).list();
                    return services.getItems().isEmpty();
                },
                String.format("Timeout waiting for all Services to be removed", context.getNamespace()));
    }

    @Then("^the Ingress \"([^\"]*)\" is ready within (\\d+) (?:minute|minutes)$")
    public void theIngressExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    Ingress ingress = context.getClient().network().v1().ingresses().inNamespace(context.getNamespace()).withName(name).get();
                    if (ingress == null) {
                        return false;
                    }
                    return ingress.getStatus() != null;
                },
                String.format("Timeout waiting for Ingress '%s' to be ready in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^no Ingress exists within (\\d+) (?:minute|minutes)$")
    public void noIngressExistsWithinMinutes(int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    IngressList ingresses = context.getClient().network().v1().ingresses().inNamespace(context.getNamespace()).list();
                    return ingresses.getItems().isEmpty();
                },
                String.format("Timeout waiting for all Ingresses to be removed", context.getNamespace()));
    }
}
