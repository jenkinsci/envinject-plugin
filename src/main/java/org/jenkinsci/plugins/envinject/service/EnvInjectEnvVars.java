package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import org.jenkinsci.plugins.envinject.EnvInjectException;
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

    public Map<String, String> processEnvVars(
            FilePath rootPath,

            EnvInjectJobPropertyInfo info,
            Map<String, String> infraEnvVarsMaster,
            Map<String, String> infraEnvVarsNode) throws IOException, InterruptedException {

        final Map<String, String> envMap = getEnvVarsFromProperties(rootPath, logger, info, infraEnvVarsMaster, infraEnvVarsNode);
        Map<String, String> variables = new LinkedHashMap<String, String>(infraEnvVarsNode);
        variables.putAll(envMap);
        return filterEnvVars(logger, infraEnvVarsNode, variables);
    }

    public void executeScript(FilePath rootPath,
                              final EnvInjectJobPropertyInfo info,
                              final Map<String, String> infraEnvVarsMaster,
                              final Map<String, String> infraEnvVarsNode,
                              Map<String, String> scriptContentEnvVars,
                              final Launcher launcher,
                              BuildListener listener) throws EnvInjectException {
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutorService scriptExecutorService;
        if (info.isLoadFilesFromMaster()) {
            scriptExecutorService = new EnvInjectScriptExecutorService(info, infraEnvVarsMaster, scriptContentEnvVars, rootPath, launcher, logger);
        } else {
            scriptExecutorService = new EnvInjectScriptExecutorService(info, infraEnvVarsNode, scriptContentEnvVars, rootPath, launcher, logger);
        }
        scriptExecutorService.executeScriptFromInfoObject();
    }

    private Map<String, String> filterEnvVars(EnvInjectLogger logger, Map<String, String> infraEnvVarsNode, Map<String, String> variables) {

        //Resolves vars each other
        resolveVars(variables, infraEnvVarsNode);

        //Remove unset variables
        return removeUnsetVars(variables);
    }

    public void resolveVars(Map<String, String> variables, Map<String, String> env) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            entry.setValue(Util.replaceMacro(entry.getValue(), env));
        }
        EnvVars.resolve(variables);
    }

    public Map<String, String> removeUnsetVars(Map<String, String> envVars) {
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
        if (value == null) {
            return false;
        }
        return value.contains("$");
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


}
