package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

public class ConditionConverter {

    public static Set<ConditionDTO> fromConditionsToConditionDTOs(Set<Condition> conditions) {
        Set<ConditionDTO> conditionDTOs = new HashSet<>(conditions.size());
        return conditions.stream().map(ConditionConverter::fromConditionToConditionDTO).collect(Collectors.toSet());
    }

    private static ConditionDTO fromConditionToConditionDTO(Condition condition) {
        return new ConditionDTO(condition.getType(),
                ConditionStatus.valueOf(condition.getStatus().name()),
                condition.getReason(),
                condition.getMessage(),
                condition.getErrorCode(),
                condition.getLastTransitionTime().toInstant().atZone(ZoneOffset.UTC));
    }
}
