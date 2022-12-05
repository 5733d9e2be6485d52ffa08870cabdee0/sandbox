package com.redhat.service.smartevents.integration.tests.logs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LogConfig {

    private static String logsOutputDirectory = System.getProperty("test.logs", "logs");

    private LogConfig() {
    }

    public static Path getLogParentDirectory(String logFolderName) {
        return Paths.get(logsOutputDirectory, logFolderName);
    }
}
