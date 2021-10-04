package com.redhat.service.bridge.executor.filters;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

import static org.assertj.core.api.Assertions.assertThat;

class FilterEvaluatorFactoryFEELTest {

    private static final FilterEvaluatorFactoryFEEL TEMPLATE_FACTORY_FEEL = new FilterEvaluatorFactoryFEEL();

    @Test
    void testStringEqualsTemplate() {
        String expected = "if data.name = \"jrota\" then \"OK\" else \"NOT_OK\"";
        String template = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringEquals("data.name", "jrota"));
        assertThat(template).isEqualTo(expected);
    }

    @Test
    void testStringContainsTemplate() {
        String expectedSingle = "if (contains (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", "[\"jrota\"]"));
        assertThat(templateSingle).isEqualTo(expectedSingle);

        String expectedMulti = "if (contains (data.name, \"jacopo\")) or (contains (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", "[\"jacopo\", \"rota\"]"));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }

    @Test
    void testStringBeginsWithTemplate() {
        String expectedSingle = "if (starts with (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", "[\"jrota\"]"));
        assertThat(templateSingle).isEqualTo(expectedSingle);

        String expectedMulti = "if (starts with (data.name, \"jacopo\")) or (starts with (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", "[\"jacopo\", \"rota\"]"));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }
}
