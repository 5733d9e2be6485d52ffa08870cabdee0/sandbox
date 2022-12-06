package com.redhat.service.smartevents.shard.operator.core.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void TestStringIsNullOrEmpty() {

        boolean result = StringUtils.stringIsNullOrEmpty(null);
        Assert.assertTrue(result);

        result = StringUtils.stringIsNullOrEmpty("");
        Assert.assertTrue(result);

    }

    @Test
    public void TestemptyToNull() {
        String testString = null;
        testString = StringUtils.emptyToNull(null);
        Assert.assertFalse(Boolean.parseBoolean(testString));

    }

}
