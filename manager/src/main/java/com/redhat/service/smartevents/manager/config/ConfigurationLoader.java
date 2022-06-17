package com.redhat.service.smartevents.manager.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows us to abstract how configuration files are loaded by other parts of the code.
 * <p>
 * This basic implementation will check two locations for the requested file:
 * - The location specified by rhose.config.external-directory (defaults to /config)
 * - The classpath
 * <p>
 * This means that we can easily override configuration on a per-environment basis by simply mounting the required file
 * at rhose.config.external-directory on the PodSpec.
 */
@ApplicationScoped
public class ConfigurationLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationLoader.class);

    private final File externalConfigDirectory;

    public ConfigurationLoader(@ConfigProperty(name = "rhose.config.external-directory", defaultValue = "/config") File externalConfigDirectory) {
        this.externalConfigDirectory = externalConfigDirectory;
    }

    public InputStream getConfigurationFileAsStream(String fileName) {

        /*
         * Check to see if the requested configuration file exists in the external configuration directory
         */
        File externalConfiguration = new File(externalConfigDirectory, fileName);
        if (externalConfiguration.exists()) {
            LOG.debug("Found requested configuration file '{}' in the external configuration directory at location '{}'.", fileName, externalConfiguration.getAbsolutePath());
            try {
                return new FileInputStream(externalConfiguration);
            } catch (Exception e) {
                throw new ConfigurationLoadException("Failed to load configuration file '" + fileName + "' from '" + externalConfiguration.getAbsolutePath() + "'", e);
            }
        }

        /*
         * Check to see if a default version of the resource is provided on the classpath
         */

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (in == null) {
            throw new ConfigurationLoadException("Requested configuration file '" + fileName + "' cannot be found in the external directory or on the classpath");
        }

        LOG.debug("Found requested configuration file '{}' on the classpath", fileName);
        return in;
    }
}
