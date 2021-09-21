package com.redhat.service.bridge.executor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

public class FilterEvaluatorFactoryFEELTest {

    private static final FilterEvaluatorFactoryFEEL TEMPLATE_FACTORY_FEEL = new FilterEvaluatorFactoryFEEL();

    @Test
    public void testStringEqualsTemplate() {
        String expected = "if data.name = \"jrota\" then \"OK\" else \"NOT_OK\"";
        String template = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringEquals("data.name", "jrota"));
        Assertions.assertEquals(expected, template);
    }

    @Test
    public void testStringContainsTemplate() {
        String expectedSingle = "if (contains (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", "[\"jrota\"]"));
        Assertions.assertEquals(expectedSingle, templateSingle);

        String expectedMulti = "if (contains (data.name, \"jacopo\")) or (contains (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", "[\"jacopo\", \"rota\"]"));
        Assertions.assertEquals(expectedMulti, templateMulti);
    }

    @Test
    public void testStringBeginsWithTemplate() {
        String expectedSingle = "if (starts with (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", "[\"jrota\"]"));
        Assertions.assertEquals(expectedSingle, templateSingle);

        String expectedMulti = "if (starts with (data.name, \"jacopo\")) or (starts with (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", "[\"jacopo\", \"rota\"]"));
        Assertions.assertEquals(expectedMulti, templateMulti);
    }
}
