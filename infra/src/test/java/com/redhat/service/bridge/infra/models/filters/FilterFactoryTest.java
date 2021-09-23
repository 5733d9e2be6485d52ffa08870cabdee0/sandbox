package com.redhat.service.bridge.infra.models.filters;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilterFactoryTest {

    @Test
    public void testStringBeginsWithFilterFactory() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FilterFactory.buildFilter(StringBeginsWith.FILTER_TYPE_NAME, "key", "test"));

        BaseFilter stringContainsFilter = FilterFactory.buildFilter(StringContains.FILTER_TYPE_NAME, "key", "[\"test\"]");
        Assertions.assertTrue(stringContainsFilter instanceof StringContains);
        Assertions.assertEquals("key", stringContainsFilter.getKey());
        Assertions.assertEquals(1, ((List<String>) stringContainsFilter.getValue()).size());
        Assertions.assertEquals("test", ((List<String>) stringContainsFilter.getValue()).get(0));
        Assertions.assertEquals("[\"test\"]", stringContainsFilter.getValueAsString());
    }

    @Test
    public void testStringContainsFilterFactory() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FilterFactory.buildFilter(StringContains.FILTER_TYPE_NAME, "key", "test"));

        BaseFilter stringBeginsFilter = FilterFactory.buildFilter(StringBeginsWith.FILTER_TYPE_NAME, "key", "[\"test\"]");
        Assertions.assertTrue(stringBeginsFilter instanceof StringBeginsWith);
        Assertions.assertEquals("key", stringBeginsFilter.getKey());
        Assertions.assertEquals(1, ((List<String>) stringBeginsFilter.getValue()).size());
        Assertions.assertEquals("test", ((List<String>) stringBeginsFilter.getValue()).get(0));
        Assertions.assertEquals("[\"test\"]", stringBeginsFilter.getValueAsString());
    }

    @Test
    public void testStringEqualsFilterFactory() {
        BaseFilter stringEqualsFilter = FilterFactory.buildFilter(StringEquals.FILTER_TYPE_NAME, "key", "test");
        Assertions.assertTrue(stringEqualsFilter instanceof StringEquals);
        Assertions.assertEquals("key", stringEqualsFilter.getKey());
        Assertions.assertEquals("test", stringEqualsFilter.getValue());
        Assertions.assertEquals("test", stringEqualsFilter.getValueAsString());
    }

    @Test
    public void testUnknownFilterType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilterFactory.buildFilter("not-a-filter-type", "key", "test"));
    }
}
