package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import org.jenkinsci.plugins.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectInfo;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
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

    public Map<String, String> getEnvVarsJobPropertyInfo(
            FilePath rootPath,
            EnvInjectJobPropertyInfo info,
            Map<String, String> infraEnvVarsMaster,
            Map<String, String> infraEnvVarsNode) throws IOException, InterruptedException {

        final Map<String, String> envMap = getEnvVarsFromProperties(rootPath, logger, info, infraEnvVarsMaster, infraEnvVarsNode);
        Map<String, String> variables = new LinkedHashMap<String, String>(infraEnvVarsNode);
        variables.putAll(envMap);
        return filterEnvVars(infraEnvVarsNode, variables);
    }

    public Map<String, String> getEnvVarsInfo(
            FilePath rootPath,
            EnvInjectInfo info,
            Map<String, String> currentEnvVars) throws IOException, InterruptedException {

        final Map<String, String> propertiesEnv = getEnvVarsFromProperties(rootPath, logger, info, currentEnvVars);
        Map<String, String> variables = new LinkedHashMap<String, String>(currentEnvVars);
        variables.putAll(propertiesEnv);
        return filterEnvVars(currentEnvVars, variables);
    }

    public void executeScript(final EnvInjectJobPropertyInfo info,
                              FilePath scriptExecutionRoot,
                              Map<String, String> infraEnvVarsMaster,
                              Map<String, String> infraEnvVarsNode,
                              Map<String, String> computedEnvVars,
                              final Launcher launcher,
                              BuildListener listener) throws EnvInjectException {
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutorService scriptExecutorService;
        if (info.isLoadFilesFromMaster()) {
            scriptExecutorService = new EnvInjectScriptExecutorService(info, scriptExecutionRoot, infraEnvVarsMaster, computedEnvVars, launcher, logger);
        } else {
            scriptExecutorService = new EnvInjectScriptExecutorService(info, scriptExecutionRoot, infraEnvVarsNode, computedEnvVars, launcher, logger);
        }
        scriptExecutorService.executeScriptFromInfoObject();
    }

    private Map<String, String> filterEnvVars(Map<String, String> previousEnvVars, Map<String, String> variables) {

        //Resolves vars each other
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

    private Map<String, String> getEnvVarsFromProperties(FilePath rootPath,
                                                         EnvInjectLogger logger,
                                                         final EnvInjectJobPropertyInfo info,
                                                         final Map<String, String> infraEnvVarsMaster,
                                                         final Map<String, String> infraEnvVarsNode) throws IOException, InterruptedException {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        if (info.isLoadFilesFromMaster()) {
            resultMap.putAll(Hudson.getInstance().getRootPath().act(new PropertiesVariablesRetriever(info, infraEnvVarsMaster, logger)));
        } else {
            resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(info, infraEnvVarsNode, logger)));
        }
        return resultMap;
    }

    private Map<String, String> getEnvVarsFromProperties(FilePath rootPath,
                                                         EnvInjectLogger logger,
                                                         final EnvInjectInfo info,
                                                         final Map<String, String> infraEnvVarsNode) throws IOException, InterruptedException {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(info, infraEnvVarsNode, logger)));
        return resultMap;
    }
}
