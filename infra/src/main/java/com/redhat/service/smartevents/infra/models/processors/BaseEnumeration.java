package com.redhat.service.smartevents.infra.models.processors;

import java.util.Arrays;
import java.util.Objects;

public interface BaseEnumeration {

    String getValue();

    static <T extends BaseEnumeration> T lookup(T[] values, String type) {
        return Arrays
                .stream(values)
                .filter(t -> Objects.equals(t.getValue(), type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("ProcessorType '%s' unknown.", type)));
    }

}
