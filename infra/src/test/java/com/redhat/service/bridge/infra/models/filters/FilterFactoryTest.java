package com.redhat.service.bridge.infra.models.filters;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class FilterFactoryTest {

    @Test
    public void testStringBeginsWithFilterFactory() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> FilterFactory.buildFilter(StringBeginsWith.FILTER_TYPE_NAME, "key", "test"));

        BaseFilter stringContainsFilter = FilterFactory.buildFilter(StringContains.FILTER_TYPE_NAME, "key", "[\"test\"]");
        assertThat(stringContainsFilter).isInstanceOf(StringContains.class);
        assertThat(stringContainsFilter.getKey()).isEqualTo("key");
        assertThat(((List<String>) stringContainsFilter.getValue()).size()).isEqualTo(1);
        assertThat(((List<String>) stringContainsFilter.getValue()).get(0)).isEqualTo("test");
        assertThat(stringContainsFilter.getValueAsString()).isEqualTo("[\"test\"]");
    }

    @Test
    public void testStringContainsFilterFactory() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> FilterFactory.buildFilter(StringContains.FILTER_TYPE_NAME, "key", "test"));

        BaseFilter stringBeginsFilter = FilterFactory.buildFilter(StringBeginsWith.FILTER_TYPE_NAME, "key", "[\"test\"]");
        assertThat(stringBeginsFilter).isInstanceOf(StringBeginsWith.class);
        assertThat(stringBeginsFilter.getKey()).isEqualTo("key");
        assertThat(((List<String>) stringBeginsFilter.getValue()).size()).isEqualTo(1);
        assertThat(((List<String>) stringBeginsFilter.getValue()).get(0)).isEqualTo("test");
        assertThat(stringBeginsFilter.getValueAsString()).isEqualTo("[\"test\"]");
    }

    @Test
    public void testStringEqualsFilterFactory() {
        BaseFilter stringEqualsFilter = FilterFactory.buildFilter(StringEquals.FILTER_TYPE_NAME, "key", "test");
        assertThat(stringEqualsFilter).isInstanceOf(StringEquals.class);
        assertThat(stringEqualsFilter.getKey()).isEqualTo("key");
        assertThat(stringEqualsFilter.getValue()).isEqualTo("test");
        assertThat(stringEqualsFilter.getValueAsString()).isEqualTo("test");
    }

    @Test
    public void testUnknownFilterType() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> FilterFactory.buildFilter("not-a-filter-type", "key", "test"));
    }
}
