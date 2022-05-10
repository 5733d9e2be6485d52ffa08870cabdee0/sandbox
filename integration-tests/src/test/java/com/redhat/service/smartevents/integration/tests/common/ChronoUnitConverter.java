package com.redhat.service.smartevents.integration.tests.common;

import java.time.temporal.ChronoUnit;

public class ChronoUnitConverter {

    public static ChronoUnit parseChronoUnits(String chronoUnits) {
        for (ChronoUnit u : ChronoUnit.values()) {
            if (u.toString().toLowerCase().startsWith(chronoUnits)) {
                return u;
            }
        }
        throw new RuntimeException("Chrono unit not found: " + chronoUnits);
    }
}
