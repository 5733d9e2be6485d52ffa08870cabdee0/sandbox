package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void TestStringIsNullOrEmpty() {
        boolean result = StringUtils.stringIsNullOrEmpty(null);
        Assertions.assertThat(Boolean.parseBoolean(String.valueOf(result))).isTrue();

        result = StringUtils.stringIsNullOrEmpty("");
        Assertions.assertThat(Boolean.parseBoolean(String.valueOf(result))).isTrue();

    }

    @Test
    public void TestemptyToNull() {
        String testString = null;
        testString = StringUtils.emptyToNull(null);
        Assertions.assertThat(Boolean.parseBoolean(testString)).isFalse();

    }

}
