package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.plugins.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectInfo;
import org.jenkinsci.plugins.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever implements FilePath.FileCallable<Map<String, String>> {

    private EnvInjectInfo info;

    private Map<String, String> currentEnvVars;

    private EnvInjectLogger logger;

    public PropertiesVariablesRetriever(EnvInjectInfo info, Map<String, String> currentEnvVars, EnvInjectLogger logger) {
        this.info = info;
        this.currentEnvVars = currentEnvVars;
        this.logger = logger;
    }

    public Map<String, String> invoke(File base, VirtualChannel channel) throws IOException, InterruptedException {
        Map<String, String> result = new LinkedHashMap<String, String>();

        try {

            PropertiesLoader loader = new PropertiesLoader();

            //Add the properties file
            if (info.getPropertiesFilePath() != null) {
                String propertiesFilePath = Util.replaceMacro(info.getPropertiesFilePath(), currentEnvVars);
                propertiesFilePath = propertiesFilePath.replace("\\", "/");
                File propertiesFile = getFile(base, propertiesFilePath);
                if (propertiesFile == null) {
                    String message = String.format("The given properties file path '%s' doesn't exist.", propertiesFilePath);
                    logger.error(message);
                    throw new EnvInjectException(message);
                }
                logger.info(String.format("Injecting as environment variables the properties file path '%s'", propertiesFilePath));
                result.putAll(loader.getVarsFromPropertiesFile(propertiesFile));
            }

            //Add the properties content
            if (info.getPropertiesContent() != null) {
                String content = Util.replaceMacro(info.getPropertiesContent(), currentEnvVars);
                logger.info(String.format("Injecting as environment variables the properties content \n '%s' \n", content));
                result.putAll(loader.getVarsFromPropertiesContent(content));
            }

        } catch (EnvInjectException envEx) {
            throw new IOException(envEx);
        }

        return result;
    }

    private File getFile(File base, String scriptFilePath) {

        File file = new File(scriptFilePath);
        if (file.exists()) {
            return file;
        }

        file = new File(base, scriptFilePath);
        return file.exists() ? file : null;
    }


}
