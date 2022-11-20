package com.redhat.service.smartevents.shard.operator.converters;

import com.redhat.service.smartevents.infra.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.resources.Condition;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConditionConverter {

    public List<ConditionDTO> fromConditionsToConditionDTOs(Set<Condition> conditions) {
        return conditions.stream().map(this::fromConditionToConditionDTO).collect(Collectors.toList());
    }

    private ConditionDTO fromConditionToConditionDTO(Condition condition) {
        ConditionDTO conditionDTO = new ConditionDTO();
        conditionDTO.setType(condition.getType());
        conditionDTO.setStatus(condition.getStatus().name());
        conditionDTO.setMessage(condition.getMessage());
        conditionDTO.setReason(condition.getReason());
        conditionDTO.setLastTransitionTime(condition.getLastTransitionTime().toString());
        conditionDTO.setErrorCode(condition.getErrorCode());
        return conditionDTO;
    }
}
