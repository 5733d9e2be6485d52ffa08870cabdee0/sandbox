
package com.redhat.service.smartevents.integration.tests.steps;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import io.cucumber.java.en.When;

public class WaitingSteps {

    @When("^wait for (\\d+) (second|seconds|minute|minutes)$")
    public void slackActionTest(int amount, String chronoUnits) throws InterruptedException {
        ChronoUnit parsedChronoUnits = parseChronoUnits(chronoUnits);
        TimeUnit.of(parsedChronoUnits).sleep(amount);
    }

    private ChronoUnit parseChronoUnits(String chronoUnits) {
        for (ChronoUnit u : ChronoUnit.values()) {
            if (u.toString().toLowerCase().startsWith(chronoUnits)) {
                return u;
            }
        }
        throw new RuntimeException("Chrono unit not found: " + chronoUnits);
    }
}
