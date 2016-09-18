package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever {

    private FilePath basePath;

    private String propertiesFilePath;

    private Map<String, String> propertiesContent;

    private Map<String, String> currentEnvVars;

    private EnvInjectLogger logger;

    public PropertiesVariablesRetriever(FilePath basePath, String propertiesFilePath, Map<String, String> propertiesContent, Map<String, String> currentEnvVars, EnvInjectLogger logger) {
        this.basePath = basePath;
        this.propertiesFilePath = propertiesFilePath;
        this.propertiesContent = propertiesContent;
        this.currentEnvVars = currentEnvVars;
        this.logger = logger;
    }

    public Map<String, String> retrieve() throws EnvInjectException {
        Map<String, String> result = new LinkedHashMap<String, String>();

        //Add the properties file
        if (propertiesFilePath != null) {
            String propertiesFilePathResolved = Util.replaceMacro(propertiesFilePath, currentEnvVars);
            propertiesFilePathResolved = propertiesFilePathResolved.replace("\\", "/");
            FilePath remotePropertiesFilePath = new FilePath(basePath, propertiesFilePathResolved);

            try {
                if (!remotePropertiesFilePath.exists()) {
                    String message = String.format("The given properties file path '%s' doesn't exist.", propertiesFilePathResolved);
                    logger.error(message);
                    String patternMessage = String.format("Missing file path was resolved from pattern '%s' .", propertiesFilePath);
                    logger.error(patternMessage);
                    throw new EnvInjectException(message);
                }

                logger.info(String.format("Injecting as environment variables the properties file path '%s'", propertiesFilePathResolved));
                result.putAll(remotePropertiesFilePath.act(new FilePath.FileCallable<Map<String, String>>() {
                    public Map<String, String> invoke(File propertiesFile, VirtualChannel virtualChannel) throws IOException, InterruptedException {
                        try {
                            return new PropertiesLoader().getVarsFromPropertiesFile(propertiesFile, currentEnvVars);
                        } catch (EnvInjectException envEx) {
                            throw new IOException(envEx.getMessage());
                        }
                    }
                }));
                logger.info("Variables injected successfully.");
            } catch (IOException e) {
                throw new EnvInjectException(e);
            } catch (InterruptedException e) {
                throw new EnvInjectException(e);
            }
        }

        //Add the properties content
        if (propertiesContent != null) {
            PropertiesGetter propertiesGetter = new PropertiesGetter();
            logger.info(String.format("Injecting as environment variables the properties content %n%s%n", propertiesGetter.getPropertiesContentFromMapObject(propertiesContent)));
            result.putAll(propertiesContent);
            logger.info("Variables injected successfully.");
        }

        return result;
    }
}
