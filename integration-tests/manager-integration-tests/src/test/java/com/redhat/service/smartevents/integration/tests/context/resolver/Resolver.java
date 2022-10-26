package com.redhat.service.smartevents.integration.tests.context.resolver;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

/**
 * A resolver helps to replace specific placeholder with information from Test context
 * 
 * First you can use the `match` method to check if your string contains the correct placeholders
 * 
 * And then use the `replace` method to update the placeholders with correct values, taken from the text context
 */
public interface Resolver {

    boolean match(String placeholder);

    String replace(String content, TestContext context);
}
