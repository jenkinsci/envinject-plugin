package org.jenkinsci.plugins.envinject.service;

import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.envinject.EnvInjectInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever implements Callable<Map<String, String>, Throwable> {

    private EnvInjectInfo info;

    private TaskListener listener;

    private Map<String, String> currentEnvVars;

    public PropertiesVariablesRetriever(EnvInjectInfo info, TaskListener listener, Map<String, String> currentEnvVars) {
        this.info = info;
        this.listener = listener;
        this.currentEnvVars = currentEnvVars;
    }

    public Map<String, String> call() throws Throwable {

        Map<String, String> result = new HashMap<String, String>();

        PropertiesFileService propertiesFileService = new PropertiesFileService(listener);

        //Process the properties file
        if (info.getPropertiesFilePath() != null) {
            String scriptFilePath = Util.replaceMacro(info.getPropertiesFilePath(), currentEnvVars);
            scriptFilePath = scriptFilePath.replace("\\", "/");
            result.putAll(propertiesFileService.getVarsFromPropertiesFilePath(scriptFilePath));
        }

        //Process the properties content
        if (info.getPropertiesContent() != null) {
            String fileContent = Util.replaceMacro(info.getPropertiesContent(), currentEnvVars);
            result.putAll(propertiesFileService.getVarsFromPropertiesContent(fileContent));
        }

        return result;
    }


}
