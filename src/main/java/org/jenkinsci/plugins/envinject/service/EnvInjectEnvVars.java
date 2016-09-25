package org.jenkinsci.plugins.envinject.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Run;
import hudson.util.VariableResolver;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEnvVars implements Serializable {

    EnvInjectLogger logger;

    public EnvInjectEnvVars(@Nonnull EnvInjectLogger logger) {
        this.logger = logger;
    }

    public Map<String, String> getEnvVarsPropertiesJobProperty(@Nonnull FilePath rootPath,
                                                               @Nonnull EnvInjectLogger logger,
                                                               boolean loadFilesFromMaster,
                                                               @CheckForNull String propertiesFilePath,
                                                               @CheckForNull Map<String, String> propertiesContent,
                                                               @Nonnull Map<String, String> infraEnvVarsMaster,
                                                               @Nonnull Map<String, String> infraEnvVarsNode) throws EnvInjectException {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        try {
            if (loadFilesFromMaster) {
                resultMap.putAll(Jenkins.getActiveInstance().getRootPath().act(
                        new PropertiesVariablesRetriever(
                                propertiesFilePath, propertiesContent, infraEnvVarsMaster, logger)));
            } else {
                resultMap.putAll(rootPath.act(
                        new PropertiesVariablesRetriever(
                                propertiesFilePath, propertiesContent, infraEnvVarsNode, logger)));
            }
        } catch (IOException e) {
            throw new EnvInjectException(e);
        } catch (InterruptedException e) {
            throw new EnvInjectException(e);
        }
        return resultMap;
    }

    @Nonnull
    public Map<String, String> getEnvVarsFileProperty(@Nonnull FilePath rootPath,
                                                      @Nonnull EnvInjectLogger logger,
                                                      @CheckForNull String propertiesFilePath,
                                                      @CheckForNull Map<String, String> propertiesContent,
                                                      @Nonnull Map<String, String> currentEnvVars) throws EnvInjectException {
        Map<String, String> resultMap = new LinkedHashMap<String, String>();
        try {
            resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(propertiesFilePath, propertiesContent, currentEnvVars, logger)));
        } catch (IOException e) {
            throw new EnvInjectException(e);
        } catch (InterruptedException e) {
            throw new EnvInjectException(e);
        }
        return resultMap;
    }

    public int executeScript(boolean loadFromMaster,
                             @CheckForNull String scriptContent,
                             @CheckForNull FilePath scriptExecutionRoot,
                             @CheckForNull String scriptFilePath,
                             @Nonnull Map<String, String> infraEnvVarsMaster,
                             @Nonnull Map<String, String> infraEnvVarsNode,
                             @Nonnull Launcher launcher,
                             @Nonnull BuildListener listener) throws EnvInjectException {

        final EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutor scriptExecutor = new EnvInjectScriptExecutor(launcher, logger);

        Map<String, String> scriptExecutionEnvVars = new HashMap<String, String>();
        scriptExecutionEnvVars.putAll(infraEnvVarsNode);

        if (loadFromMaster) {
            Map<String, String> scriptPathExecutionEnvVars = new HashMap<String, String>();
            scriptPathExecutionEnvVars.putAll(infraEnvVarsMaster);
            if (scriptFilePath != null) {
                String scriptFilePathResolved = Util.replaceMacro(scriptFilePath, scriptPathExecutionEnvVars);
                String content = null;
                try {
                    content = FileUtils.readFileToString(new File(scriptFilePathResolved));
                } catch (IOException e) {
                    throw new EnvInjectException("Failed to load script from master", e);
                }
                return scriptExecutor.executeScriptSection(scriptExecutionRoot, null, content, scriptPathExecutionEnvVars, scriptExecutionEnvVars);
            }
            return scriptExecutor.executeScriptSection(scriptExecutionRoot, null, scriptContent, scriptPathExecutionEnvVars, scriptExecutionEnvVars);

        } else {
            return scriptExecutor.executeScriptSection(scriptExecutionRoot, scriptFilePath, scriptContent, scriptExecutionEnvVars, scriptExecutionEnvVars);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> executeAndGetMapGroovyScript(
            @Nonnull EnvInjectLogger logger, 
            @CheckForNull String scriptContent, 
            @Nonnull Map<String, String> envVars) throws EnvInjectException, AbortException {

        if (scriptContent == null) {
            return new HashMap<String, String>();
        }

        if (scriptContent.trim().length() == 0) {
            return new HashMap<String, String>();
        }

        logger.info("Evaluating the Groovy script content");

        Binding binding = new Binding();
        String jobName = envVars.get("JOB_NAME");
        if (jobName != null) {
            Item job = Jenkins.getActiveInstance().getItemByFullName(jobName);
            binding.setProperty("currentJob", job);
            String b = envVars.get("BUILD_NUMBER");
            if (b != null && job instanceof AbstractProject) {
                Run r = ((AbstractProject) job).getBuildByNumber(Integer.parseInt(b));
                binding.setProperty("currentBuild", r);
            }
        }

        GroovyShell groovyShell = new GroovyShell(Jenkins.getActiveInstance().getPluginManager().uberClassLoader, binding);
        for (Map.Entry<String, String> entryVariable : envVars.entrySet()) {
            groovyShell.setVariable(entryVariable.getKey(), entryVariable.getValue());
        }
        groovyShell.setVariable("out", logger.getListener().getLogger());
        groovyShell.setVariable("currentListener", logger.getListener());

        Object groovyResult = groovyShell.evaluate(scriptContent);
        if (groovyResult != null && !(groovyResult instanceof Map)) {
            throw new AbortException("The evaluated Groovy script must return a Map object.");
        }

        Map<String, String> result = new HashMap<String, String>();
        if (groovyResult == null) {
            return result;
        }

        for (Map.Entry entry : (((Map<Object, Object>) groovyResult).entrySet())) {
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return result;
    }

    public int executeScript(
            @CheckForNull String scriptContent,
            @CheckForNull FilePath scriptExecutionRoot,
            @CheckForNull String scriptFilePath,
            @Nonnull Map<String, String> envVars,
            @Nonnull Launcher launcher,
            @Nonnull BuildListener listener) throws EnvInjectException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutor scriptExecutor = new EnvInjectScriptExecutor(launcher, logger);
        return scriptExecutor.executeScriptSection(scriptExecutionRoot, scriptFilePath, scriptContent, envVars, envVars);
    }

    @Nonnull
    public Map<String, String> getMergedVariables(
            @Nonnull Map<String, String> infraEnvVars, 
            @Nonnull Map<String, String> propertiesEnvVars) {
        return getMergedVariables(infraEnvVars, propertiesEnvVars, new HashMap<String, String>(), new HashMap<String, String>());
    }

    @Nonnull
    public Map<String, String> getMergedVariables(@Nonnull Map<String, String> infraEnvVars,
                                                  @Nonnull Map<String, String> propertiesEnvVars,
                                                  @Nonnull Map<String, String> groovyMapEnvVars,
                                                  @Nonnull Map<String, String> contribEnvVars) {

        //0-- Pre-resolve infraEnv vars
        resolveVars(infraEnvVars, infraEnvVars);

        //1--Resolve properties against infraEnvVars
        resolveVars(propertiesEnvVars, infraEnvVars);

        //2--Resolve properties against groovyEnvVars
        resolveVars(propertiesEnvVars, groovyMapEnvVars);

        //3--Resolve properties against contribEnvVars
        resolveVars(propertiesEnvVars, contribEnvVars);

        //4-- Get All variables in order (infraEnvVars, groovyEnvVars, contribEnvVars, properties)
        Map<String, String> variables = new LinkedHashMap<String, String>(infraEnvVars);
        variables.putAll(groovyMapEnvVars);
        variables.putAll(contribEnvVars);
        variables.putAll(propertiesEnvVars);

        return variables;
    }

    public void resolveVars(@Nonnull Map<String, String> variables, 
            @Nonnull Map<String, String> env) {

        //Resolve variables against env
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = Util.replaceMacro(entry.getValue(), env);
            entry.setValue(value);
        }

        //Resolve variables against variables itself
        boolean stopToResolveVars = false;
        int nbUnresolvedVar = 0;

        while (!stopToResolveVars) {
            int previousNbUnresolvedVar = nbUnresolvedVar;
            nbUnresolvedVar = 0;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                Map<String, String> resolveVariables = new HashMap<String, String>(variables);
                // avoid self reference expansion
                // FOO=$FOO-X -> FOO=$FOO-X-X -> FOO=$FOO-X-X-X etc.
                resolveVariables.remove(entry.getKey());
                String value = Util.replaceMacro(entry.getValue(), resolveVariables);
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

    @Nonnull
    public Map<String, String> removeUnsetVars(@Nonnull Map<String, String> envVars) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {

            String value = entry.getValue();

            value = removeUnsetVars(value);

            if (!isUnresolvedVar(value)) {
                result.put(entry.getKey(), removeEscapeDollar(value));
            } else {
                logger.info(String.format("Unset unresolved '%s' variable.", entry.getKey()));
            }
        }
        return result;
    }

    @Nullable
    private String removeUnsetVars(@CheckForNull String value) {

        if (value == null) {
            return null;
        }

        if (value.length() == 0) {
            return value;
        }

        if (!value.contains("$") || value.contains("\\$")) {
            return value;
        }

        return Util.replaceMacro(value, new VariableResolver<String>() {

            public String resolve(String name) {
                return "";
            }
        });

    }

    private boolean isUnresolvedVar(@CheckForNull String value) {

        if (value == null) {
            return true;
        }

        //Empty environment variables are acceptable
        if (value.trim().length() == 0) {
            return false;
        }

        return value.contains("$") && !value.contains("\\$");
    }

    private String removeEscapeDollar(@CheckForNull String value) {
        if (value == null) {
            return  null;
        }
        
        //We replace escaped $ unless we are on Windows (Unix only)
        if ('/' == File.separatorChar) { //unix test
            return value.replace("\\$", "$");
        }
        return value;
    }

}
