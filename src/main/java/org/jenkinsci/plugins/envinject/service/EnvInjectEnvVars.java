package org.jenkinsci.plugins.envinject.service;

import groovy.lang.Binding;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.RestrictedSince;
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
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.envinject.EnvInjectPluginConfiguration;
import org.jenkinsci.plugins.envinject.util.EnvInjectExceptionFormatter;

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
                // Even if the propertiesFilePath is null, we do not want to allow loading
                // from the master, because it may expose sensitive Environment Variables
                if (!EnvInjectPluginConfiguration.getOrFail().isEnableLoadingFromMaster()) {
                    throw EnvInjectExceptionFormatter.forProhibitedLoadFromMaster(propertiesFilePath);
                }
                resultMap.putAll(Jenkins.get().getRootPath().act(
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

        if (scriptContent == null && scriptFilePath == null) {
            // Neither option is specified, no sense in running the logic
            return 0;
        }
        
        final EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutor scriptExecutor = new EnvInjectScriptExecutor(launcher, logger);

        Map<String, String> scriptExecutionEnvVars = new HashMap<String, String>();
        scriptExecutionEnvVars.putAll(infraEnvVarsNode);
        
        if (loadFromMaster) {
            if (!EnvInjectPluginConfiguration.getOrFail().isEnableLoadingFromMaster()) {
                throw EnvInjectExceptionFormatter.forProhibitedLoadFromMaster(scriptFilePath);
            }
            
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

    /**
     * Executes a groovy script and returns the result as a map.
     * The script will be executed in a {@link SecureGroovyScript} without the sandbox,
     * so chances are low that it will succeed unless an exact copy has already been approved.
     * @param logger the logger
     * @param scriptContent the script
     * @param envVars variables to bind to the script
     * @return the result as a map
     * @throws EnvInjectException if so
     * @throws AbortException if so
     * @deprecated use {@link #executeGroovyScript(EnvInjectLogger, SecureGroovyScript, Map)} instead.
     * @see #executeGroovyScript(EnvInjectLogger, SecureGroovyScript, Map)
     * @since 1.38 - Initial implementation
     * @since 2.0 - Uses Secure Groovy Script. It will require approvals for non-admin users
     *            It is also restricted.
     * 
     */
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.0")
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

        logger.info("Using the deprecated API (EnvInjectEnvVars#executeAndGetMapGroovyScript()). Starting from EnvInject 2.0 the call may require the script approval for a non-admin user.");
        SecureGroovyScript script = new SecureGroovyScript(scriptContent, false, null);
        return executeGroovyScript(logger, script, envVars);
    }

    /**
     * Executes the {@link SecureGroovyScript} and returns a map generated by the script/
     * @param logger a logger
     * @param script the script
     * @param envVars any variables to bind to the script's context
     * @return the map
     * @throws EnvInjectException for any exceptions generated by the script execution
     * @throws AbortException if something is badly wrong.
     * @since 2.0
     */
    @Nonnull
    @Restricted(NoExternalUse.class)
    public Map<String, String> executeGroovyScript(
            @Nonnull EnvInjectLogger logger,
            @CheckForNull SecureGroovyScript script,
            @Nonnull Map<String, String> envVars) throws EnvInjectException, AbortException {

        final Jenkins jenkins = Jenkins.get();
        
        if (script == null) {
            return new HashMap<String, String>();
        }

        if (script.getScript().trim().length() == 0) {
            return new HashMap<String, String>();
        }

        logger.info("Evaluating the Groovy script content");

        Binding binding = new Binding();
        String jobName = envVars.get("JOB_NAME");
        if (jobName != null) {
            Item job = jenkins.getItemByFullName(jobName);
            binding.setProperty("currentJob", job);
            String b = envVars.get("BUILD_NUMBER");
            //TODO: Use Job instead
            if (b != null && job instanceof AbstractProject) {
                Run r = ((AbstractProject) job).getBuild(b);
                binding.setProperty("currentBuild", r);
            }
        }

        for (Map.Entry<String, String> entryVariable : envVars.entrySet()) {
            binding.setVariable(entryVariable.getKey(), entryVariable.getValue());
        }
        binding.setVariable("out", logger.getListener().getLogger());
        binding.setVariable("currentListener", logger.getListener());

        final Object groovyResult;
        try {
            groovyResult = script.evaluate(jenkins.getPluginManager().uberClassLoader, binding);
        } catch (Exception e) {
            throw new EnvInjectException("Failed to evaluate the script", e);
        }
        if (groovyResult != null && !(groovyResult instanceof Map)) {
            throw new AbortException("The evaluated Groovy script must return a Map object.");
        }

        Map<String, String> result = new HashMap<String, String>();
        if (groovyResult == null) {
            return result;
        }
        Map<?,?> mapResult = (Map<?, ?>)groovyResult;
        for (Map.Entry<?, ?> entry : mapResult.entrySet()) {
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
