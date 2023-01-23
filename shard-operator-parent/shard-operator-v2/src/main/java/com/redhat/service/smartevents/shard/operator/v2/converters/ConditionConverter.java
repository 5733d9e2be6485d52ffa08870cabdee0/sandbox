package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
        return new ConditionDTO(condition.getType(),
                ConditionStatus.fromString(condition.getStatus().name()),
                condition.getReason(),
                condition.getMessage(),
                condition.getErrorCode(),
                fromDate(condition.getLastTransitionTime()));
    }

    private static ZonedDateTime fromDate(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant().atZone(ZoneOffset.UTC);
    }
}
