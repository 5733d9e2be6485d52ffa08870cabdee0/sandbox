package com.redhat.service.smartevents.executor.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.filters.ObjectMapperFactory;
import com.redhat.service.smartevents.infra.models.filters.StringBeginsWith;
import com.redhat.service.smartevents.infra.models.filters.StringContains;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.filters.ValuesIn;

public class FilterEvaluatorFactoryFEEL implements FilterEvaluatorFactory {

    public static final String IS_VALID = "OK";
    public static final String IS_INVALID = "NOT_OK";
    private static final String TEMPLATE = "if %s then \"" + IS_VALID + "\" else \"" + IS_INVALID + "\"";

    @Override
    public FilterEvaluator build(Set<BaseFilter> filters) {
        Set<String> templates = filters == null ? null : filters.stream().map(this::getTemplateByFilterType).collect(Collectors.toSet());
        return new FilterEvaluatorFEEL(templates);
    }

    protected String getTemplateByFilterType(BaseFilter filter) {
        return String.format(TEMPLATE, getFilterCondition(filter));
    }

    private String getFilterCondition(BaseFilter filter) {
        switch (filter.getType()) {
            case StringEquals.FILTER_TYPE_NAME:
                return String.format("%s = \"%s\"", filter.getKey(), filter.getValue());
            case StringContains.FILTER_TYPE_NAME:
                return getFilterConditionForListValues("(contains (%s, %s))", filter.getKey(), ((StringContains) filter).getValue());
            case StringBeginsWith.FILTER_TYPE_NAME:
                return getFilterConditionForListValues("(starts with (%s, %s))", filter.getKey(), ((StringBeginsWith) filter).getValue());
            case ValuesIn.FILTER_TYPE_NAME:
                return getFilterConditionForListValues("%s = %s", filter.getKey(), ((ValuesIn) filter).getValue());
            default:
                throw new IllegalArgumentException("Filter type " + filter.getType() + " is not supported by FEELTemplateFactory.");
        }
    }

    private String getFilterConditionForListValues(String singleFormatTemplate, String key, List<?> values) {
        List<String> conditions = new ArrayList<>();
        ObjectMapper objectMapper = ObjectMapperFactory.get();
        for (Object value : values) {
            try {
                conditions.add(String.format(singleFormatTemplate, key, objectMapper.writeValueAsString(value)));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Value " + value + " cannot be converted to string", e);
            }
        }
        return String.join(" or ", conditions);
    }

}
