package com.redhat.service.bridge.executor.filters;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.api.models.filters.StringContains;
import com.redhat.service.bridge.infra.api.models.filters.StringEquals;
import com.redhat.service.bridge.infra.api.models.filters.ValuesIn;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterEvaluatorFactoryFEELTest {

    private static final FilterEvaluatorFactoryFEEL TEMPLATE_FACTORY_FEEL = new FilterEvaluatorFactoryFEEL();

    @Test
    public void testStringEqualsTemplate() {
        String expected = "if data.name = \"jrota\" then \"OK\" else \"NOT_OK\"";
        String template = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringEquals("data.name", "jrota"));
        assertThat(template).isEqualTo(expected);
    }

    @Test
    public void testStringContainsTemplate() {
        String expectedSingle = "if (contains (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", Arrays.asList("jrota")));
        assertThat(templateSingle).isEqualTo(expectedSingle);

        String expectedMulti = "if (contains (data.name, \"jacopo\")) or (contains (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringContains("data.name", Arrays.asList("jacopo", "rota")));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }

    @Test
    public void testStringBeginsWithTemplate() {
        String expectedSingle = "if (starts with (data.name, \"jrota\")) then \"OK\" else \"NOT_OK\"";
        String templateSingle = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", Arrays.asList("jrota")));
        assertThat(templateSingle).isEqualTo(expectedSingle);

        String expectedMulti = "if (starts with (data.name, \"jacopo\")) or (starts with (data.name, \"rota\")) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringBeginsWith("data.name", Arrays.asList("jacopo", "rota")));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }

    @Test
    public void valuesInWithTemplate() {
        String expectedMulti = "if data.name = \"jacopo\" or data.name = \"rota\" or data.name = 2 then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new ValuesIn("data.name", Arrays.asList("jacopo", "rota", 2)));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }
}
