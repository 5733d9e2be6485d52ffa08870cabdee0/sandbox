package com.redhat.service.smartevents.integration.tests.resources;

import software.tnb.aws.common.account.AWSAccount;
import software.tnb.common.account.Accounts;

public class AwsAccount {

    private AwsAccount() {

    }

    public static String getAccessKey() {
        return Accounts.get(AWSAccount.class).accessKey();
    }

    public static String getSecretKey() {
        return Accounts.get(AWSAccount.class).secretKey();
    }

    public static String getRegion() {
        return Accounts.get(AWSAccount.class).region();
    }
}
