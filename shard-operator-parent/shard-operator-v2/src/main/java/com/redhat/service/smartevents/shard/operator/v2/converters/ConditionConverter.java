package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

public class ConditionConverter {

    public static List<ConditionDTO> fromConditionsToConditionDTOs(Set<Condition> conditions) {
        return conditions.stream().map(ConditionConverter::fromConditionToConditionDTO).collect(Collectors.toList());
    }

    private static ConditionDTO fromConditionToConditionDTO(Condition condition) {
        ConditionDTO conditionDTO = new ConditionDTO(condition.getType(),
                ConditionStatus.fromString(condition.getStatus().name()),
                condition.getReason(),
                condition.getMessage(),
                condition.getErrorCode());
        if (condition.getLastTransitionTime() != null) {
            conditionDTO.setLastTransitionTime(condition.getLastTransitionTime().toInstant().atZone(ZoneOffset.UTC));
        }
        return conditionDTO;
    }
}
