package com.redhat.service.bridge.shard.operator.cucumber.logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Strings;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Used to store pod logs in a namespace.
 */
public class LogCollector {

    private LogCollector() {
    }

    /**
     * Store logs of all pods in provided namespace
     * 
     * @param client
     * @param logFolderName Name of a folder used to store logs
     * @param namespace
     * @throws IOException In case log cannot be stored on filesystem for any reason
     */
    public static void storeNamespacePodLogs(OpenShiftClient client, String logFolderName, String namespace) throws IOException {
        Path logParentDirectory = LogConfig.getLogParentDirectory(logFolderName);
        Files.createDirectories(logParentDirectory);

        for (Pod pod : client.pods().inNamespace(namespace).list().getItems()) {
            for (Container container : pod.getSpec().getContainers()) {
                String log = "";
                if (Strings.isNullOrEmpty(container.getName())) {
                    // Retrieve default pod log if container name is not specified
                    log = client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).getLog();
                } else {
                    log = client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).inContainer(container.getName()).getLog();
                }
                Files.write(getLogFilePath(logParentDirectory, pod, container), log.getBytes());
            }
        }
    }

    /**
     * @param logParentDirectory Path to a folder used to store logs
     * @param pod
     * @param container
     * @return Path pointing to the log file, name containing pod name and optionally container name
     */
    private static Path getLogFilePath(Path logParentDirectory, Pod pod, Container container) {
        String logFileName = String.format("%s.log", pod.getMetadata().getName());
        if (!Strings.isNullOrEmpty(container.getName())) {
            logFileName = String.format("%s-%s.log", pod.getMetadata().getName(), container.getName());
        }
        return logParentDirectory.resolve(Paths.get(logFileName));
    }
}
