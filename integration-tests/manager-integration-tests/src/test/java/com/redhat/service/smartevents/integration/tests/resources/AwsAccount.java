package com.redhat.service.smartevents.integration.tests.resources;

import software.tnb.aws.common.account.AWSAccount;
import software.tnb.common.account.AccountFactory;

public class AwsAccount {

    private AwsAccount() {

    }

    public static String getAccessKey() {
        return AccountFactory.create(AWSAccount.class).accessKey();
    }

    public static String getSecretKey() {
        return AccountFactory.create(AWSAccount.class).secretKey();
    }

    public static String getRegion() {
        return AccountFactory.create(AWSAccount.class).region();
    }
}
