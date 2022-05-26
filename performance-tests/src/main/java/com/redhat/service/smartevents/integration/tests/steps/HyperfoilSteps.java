package com.redhat.service.smartevents.integration.tests.steps;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

import io.cucumber.java.en.When;

import software.tnb.common.service.ServiceFactory;
import software.tnb.hyperfoil.service.Hyperfoil;

public class HyperfoilSteps {

    @RegisterExtension
    public static Hyperfoil hyperfoil = ServiceFactory.create(Hyperfoil.class);

    private TestContext context;

    public HyperfoilSteps(TestContext context) {
        this.context = context;
    }

    @When("^start and wait for benchmark \"([^\"]*)\" in Hyperfoil$")
    public void createNewBridge(String benchmarkPathOrUri) {
        hyperfoil.getValidation().startAndWaitForBenchmark(benchmarkPathOrUri);
    }

}
