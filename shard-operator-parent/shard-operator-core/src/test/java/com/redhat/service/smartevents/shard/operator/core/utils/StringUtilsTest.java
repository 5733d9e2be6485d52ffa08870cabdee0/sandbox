package com.redhat.service.smartevents.shard.operator.core.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void TestStringIsNullOrEmpty(){

        boolean result = StringUtils.stringIsNullOrEmpty(null);
        Assertions.assertTrue(result);


        result = StringUtils.stringIsNullOrEmpty("");
        Assertions.assertTrue(result);


    }
    @Test
    public void TestemptyToNull(){
        String testString = null;
        testString = StringUtils.emptyToNull(null);
         Assertions.assertFalse(Boolean.parseBoolean(testString));



    }

}


