package com.redhat.service.bridge.shard.operator;

import java.util.regex.Pattern;

import com.redhat.service.bridge.shard.operator.utils.RFC1123Sanitizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RFC1123SanitizerTest {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");

    @Test
    public void sanitizeUpperCases(){
        assertThat(PATTERN.matcher(RFC1123Sanitizer.sanitize("hello")).matches()).isTrue();
        assertThat(PATTERN.matcher(RFC1123Sanitizer.sanitize("Hello")).matches()).isTrue();
        assertThat(PATTERN.matcher(RFC1123Sanitizer.sanitize("fdhsHeh")).matches()).isTrue();
    }

    @Test
    @Disabled("https://issues.redhat.com/browse/MGDOBR-100")
    public void sanitizeIllegalFirstChar(){
        assertThat(PATTERN.matcher(RFC1123Sanitizer.sanitize("-hello")).matches()).isTrue();
    }

    //TODO: Add more tests with https://issues.redhat.com/browse/MGDOBR-100
}
