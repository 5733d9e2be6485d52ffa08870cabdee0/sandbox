
package com.redhat.service.smartevents.integration.tests.steps;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.redhat.service.smartevents.integration.tests.common.ChronoUnitConverter;

import io.cucumber.java.en.When;

public class WaitingSteps {

    @When("^wait for (\\d+) (second|seconds|minute|minutes)$")
    public void slackActionTest(int amount, String chronoUnits) throws InterruptedException {
        ChronoUnit parsedChronoUnits = ChronoUnitConverter.parseChronoUnits(chronoUnits);
        TimeUnit.of(parsedChronoUnits).sleep(amount);
    }
}
