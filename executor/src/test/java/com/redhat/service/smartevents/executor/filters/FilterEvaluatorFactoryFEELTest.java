package com.redhat.service.smartevents.executor.filters;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.filters.NumberIn;
import com.redhat.service.smartevents.infra.models.filters.StringBeginsWith;
import com.redhat.service.smartevents.infra.models.filters.StringContains;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.filters.StringIn;

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
    public void numberInWithTemplate() {
        String expectedMulti = "if list contains ([2.0, 3.0], data.name) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new NumberIn("data.name", Arrays.asList(2d, 3d)));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }

    @Test
    public void stringInWithTemplate() {
        String expectedMulti = "if list contains ([\"jacopo\", \"rota\"], data.name) then \"OK\" else \"NOT_OK\"";
        String templateMulti = TEMPLATE_FACTORY_FEEL.getTemplateByFilterType(new StringIn("data.name", Arrays.asList("jacopo", "rota")));
        assertThat(templateMulti).isEqualTo(expectedMulti);
    }
}
