package com.redhat.service.smartevents.executor;

public class CloudEventExtension {

    public static String adjustExtensionName(String original) {
        StringBuilder sb = new StringBuilder();

        String lowerCase = original.toLowerCase();

        for (int i = 0; i != lowerCase.length(); ++i) {
            char c = lowerCase.charAt(i);
            if (isValidExtensionNameCharacter(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static boolean isValidExtensionNameCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }
}
