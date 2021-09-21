package com.redhat.service.bridge.executor;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

@ApplicationScoped
public class TemplateFactoryFEEL {

    public static final String IS_VALID = "OK";
    public static final String IS_INVALID = "NOT_OK";
    private static final String TEMPLATE = "if %s then \"" + IS_VALID + "\" else \"" + IS_INVALID + "\"";

    public String build(BaseFilter filter) {
        return String.format(TEMPLATE, getTemplateByFilterType(filter));
    }

    private String getTemplateByFilterType(BaseFilter filter) {
        switch (filter.getType()) {
            case StringEquals.FILTER_TYPE_NAME:
                return String.format("%s = \"%s\"", filter.getKey(), filter.getValueAsString());
            case StringContains.FILTER_TYPE_NAME:
                return buildTemplateForListValues("(contains (%s, \"%s\"))", filter);
            case StringBeginsWith.FILTER_TYPE_NAME:
                return buildTemplateForListValues("(starts with (%s, \"%s\"))", filter);
            default:
                throw new IllegalArgumentException("Filter type " + filter.getType() + " is not supported by FEELTemplateFactory.");
        }
    }

    @SuppressWarnings("unchecked")
    private String buildTemplateForListValues(String singleFormatTemplate, BaseFilter filter) {
        List<String> conditions = new ArrayList<>();
        for (String value : (List<String>) filter.getValue()) {
            conditions.add(String.format(singleFormatTemplate, filter.getKey(), value));
        }
        return String.join(" or ", conditions);
    }

}
