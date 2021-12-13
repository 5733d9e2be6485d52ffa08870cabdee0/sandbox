package com.redhat.service.bridge.shard.operator.cucumber.logs;

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
