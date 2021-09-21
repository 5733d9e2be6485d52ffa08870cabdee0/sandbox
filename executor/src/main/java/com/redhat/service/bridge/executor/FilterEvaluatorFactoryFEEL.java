package com.redhat.service.bridge.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

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
                return String.format("%s = \"%s\"", filter.getKey(), filter.getValueAsString());
            case StringContains.FILTER_TYPE_NAME:
                return getFilterConditionForListValues("(contains (%s, \"%s\"))", filter);
            case StringBeginsWith.FILTER_TYPE_NAME:
                return getFilterConditionForListValues("(starts with (%s, \"%s\"))", filter);
            default:
                throw new IllegalArgumentException("Filter type " + filter.getType() + " is not supported by FEELTemplateFactory.");
        }
    }

    @SuppressWarnings("unchecked")
    private String getFilterConditionForListValues(String singleFormatTemplate, BaseFilter filter) {
        List<String> conditions = new ArrayList<>();
        for (String value : (List<String>) filter.getValue()) {
            conditions.add(String.format(singleFormatTemplate, filter.getKey(), value));
        }
        return String.join(" or ", conditions);
    }

}
