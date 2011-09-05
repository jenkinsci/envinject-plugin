package org.jenkinsci.plugins.envinject.service;

import hudson.Util;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.envinject.EnvInjectInfo;
import org.jenkinsci.plugins.envinject.EnvInjectLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever implements Callable<Map<String, String>, Throwable> {

    private EnvInjectInfo info;

    private Map<String, String> currentEnvVars;

    private EnvInjectLogger logger;

    public PropertiesVariablesRetriever(EnvInjectInfo info, Map<String, String> currentEnvVars, EnvInjectLogger logger) {
        this.info = info;
        this.currentEnvVars = currentEnvVars;
        this.logger = logger;
    }

    public Map<String, String> call() throws Throwable {

        Map<String, String> result = new HashMap<String, String>();

        PropertiesFileService propertiesFileService = new PropertiesFileService();

        //Add the properties file
        if (info.getPropertiesFilePath() != null) {
            String scriptFilePath = Util.replaceMacro(info.getPropertiesFilePath(), currentEnvVars);
            scriptFilePath = scriptFilePath.replace("\\", "/");
            logger.info(String.format("Injecting as environment variables the properties file path '%s'", scriptFilePath));
            result.putAll(propertiesFileService.getVarsFromPropertiesFilePath(scriptFilePath));
        }

        //Add the properties content
        if (info.getPropertiesContent() != null) {
            String content = Util.replaceMacro(info.getPropertiesContent(), currentEnvVars);
            logger.info(String.format("Injecting as environment variables the properties content \n '%s' \n", content));
            result.putAll(propertiesFileService.getVarsFromPropertiesContent(content));
        }

        return result;
    }


}
