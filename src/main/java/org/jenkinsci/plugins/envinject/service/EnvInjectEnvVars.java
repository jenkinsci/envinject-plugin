package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import org.jenkinsci.plugins.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEnvVars implements Serializable {

    EnvInjectLogger logger;

    public EnvInjectEnvVars(EnvInjectLogger logger) {
        this.logger = logger;
    }

    public Map<String, String> getEnvVarsPropertiesJobProperty(FilePath rootPath,
                                                               EnvInjectLogger logger,
                                                               boolean loadFilesFromMaster,
                                                               String propertiesFilePath,
                                                               String propertiesContent,
                                                               Map<String, String> infraEnvVarsMaster,
                                                               Map<String, String> infraEnvVarsNode) throws IOException, InterruptedException {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        if (loadFilesFromMaster) {
            resultMap.putAll(Hudson.getInstance().getRootPath().act(new PropertiesVariablesRetriever(propertiesFilePath, propertiesContent, infraEnvVarsMaster, logger)));
        } else {
            resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(propertiesFilePath, propertiesContent, infraEnvVarsNode, logger)));
        }
        return resultMap;
    }

    public Map<String, String> getEnvVarsPropertiesProperty(FilePath rootPath,
                                                            EnvInjectLogger logger,
                                                            String propertiesFilePath,
                                                            String propertiesContent,
                                                            Map<String, String> currentEnvVars) throws IOException, InterruptedException {
        Map<String, String> resultMap = new LinkedHashMap<String, String>();
        resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(propertiesFilePath, propertiesContent, currentEnvVars, logger)));
        return resultMap;
    }

    public int executeScript(boolean loadFromMaster,
                             String scriptContent,
                             FilePath scriptExecutionRoot,
                             String scriptFilePath,
                             Map<String, String> infraEnvVarsMaster,
                             Map<String, String> infraEnvVarsNode,
                             Map<String, String> propertiesEnvVars,
                             Launcher launcher,
                             BuildListener listener) throws EnvInjectException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutor scriptExecutor = new EnvInjectScriptExecutor(launcher, logger);

        Map<String, String> scriptExecutionEnvVars = new HashMap<String, String>();
        scriptExecutionEnvVars.putAll(infraEnvVarsNode);
        scriptExecutionEnvVars.putAll(propertiesEnvVars);

        if (loadFromMaster) {
            Map<String, String> scriptPathExecutionEnvVars = new HashMap<String, String>();
            scriptPathExecutionEnvVars.putAll(infraEnvVarsMaster);
            scriptPathExecutionEnvVars.putAll(propertiesEnvVars);
            return scriptExecutor.executeScriptSection(scriptExecutionRoot, scriptFilePath, scriptContent, scriptPathExecutionEnvVars, scriptExecutionEnvVars);
        } else {
            return scriptExecutor.executeScriptSection(scriptExecutionRoot, scriptFilePath, scriptContent, scriptExecutionEnvVars, scriptExecutionEnvVars);
        }
    }

    public int executeScript(
            String scriptContent,
            FilePath scriptExecutionRoot,
            String scriptFilePath,
            Map<String, String> envVars,
            Launcher launcher,
            BuildListener listener) throws EnvInjectException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutor scriptExecutor = new EnvInjectScriptExecutor(launcher, logger);
        return scriptExecutor.executeScriptSection(scriptExecutionRoot, scriptFilePath, scriptContent, envVars, envVars);
    }


    public Map<String, String> getMergedVariables(Map<String, String> infraEnvVars, Map<String, String> propertiesEnvVars) {
        Map<String, String> variables = new LinkedHashMap<String, String>(infraEnvVars);
        variables.putAll(propertiesEnvVars);
        return filterEnvVars(variables, variables);
    }

    private Map<String, String> filterEnvVars(Map<String, String> previousEnvVars, Map<String, String> variables) {

        //Resolve vars each other
        resolveVars(variables, previousEnvVars);

        //Remove unset variables
        return removeUnsetVars(variables);
    }

    private void resolveVars(Map<String, String> variables, Map<String, String> env) {

        //Resolve variables against env
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            entry.setValue(Util.replaceMacro(entry.getValue(), env));
        }

        //Resolve variables against variables itself
        boolean stopToResolveVars = false;
        int nbUnresolvedVar = 0;

        while (!stopToResolveVars) {
            int previousNbUnresolvedVar = nbUnresolvedVar;
            nbUnresolvedVar = 0;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String value = Util.replaceMacro(entry.getValue(), variables);
                entry.setValue(value);
                if (isUnresolvedVar(value)) {
                    nbUnresolvedVar++;
                }
            }
            if (previousNbUnresolvedVar == nbUnresolvedVar) {
                stopToResolveVars = true;
            }
        }
    }

    private Map<String, String> removeUnsetVars(Map<String, String> envVars) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            if (!isUnresolvedVar(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                logger.info(String.format("Unset unresolved '%s' variable.", entry.getKey()));
            }
        }
        return result;
    }

    private boolean isUnresolvedVar(String value) {
        return value != null && value.contains("$");
    }

}
