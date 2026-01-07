package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.envinject.util.RunHelper;


/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionSetter implements Serializable {

    @CheckForNull
    private FilePath rootPath;

    public EnvInjectActionSetter(@CheckForNull FilePath rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * @deprecated Use {@link #addEnvVarsToRun(hudson.model.Run, java.util.Map)}
     */
    @Deprecated
    public void addEnvVarsToEnvInjectBuildAction(@NonNull AbstractBuild<?, ?> build, @CheckForNull Map<String, String> envMap) 
            throws EnvInjectException, IOException, InterruptedException {
        addEnvVarsToRun(build, envMap);
    }
    
    /**
     * Adds EnvironmentVariables to the run.
     * {@link EnvInjectPluginAction} will be created on-demand.
     * @param run Run
     * @param envMap Environment variables to be added or overridden
     * @throws EnvInjectException Injection failure
     * @throws IOException Remote operation failure
     * @throws InterruptedException Remote call is interrupted
     * @since 2.1
     */
    public void addEnvVarsToRun(@NonNull Run<?, ?> run, @CheckForNull Map<String, String> envMap) 
            throws EnvInjectException, IOException, InterruptedException {

        EnvInjectPluginAction envInjectAction = run.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            envInjectAction.overrideAll(RunHelper.getSensitiveBuildVariables(run), envMap);
        } else {
            if (rootPath != null) {
                envInjectAction = new EnvInjectPluginAction(rootPath.act(new MapEnvInjectExceptionMasterToSlaveCallable()));
                envInjectAction.overrideAll(RunHelper.getSensitiveBuildVariables(run), envMap);
                run.addAction(envInjectAction);
            }
        }
    }

    private static class MapEnvInjectExceptionMasterToSlaveCallable extends MasterToSlaveCallable<Map<String, String>, EnvInjectException> {
        private static final long serialVersionUID = 1L;

        @Override
        public Map<String, String> call() throws EnvInjectException {
            HashMap<String, String> result = new HashMap<String, String>();
            result.putAll(org.jenkinsci.plugins.envinject.EnvInjectGlobalStorage.getMergedVars(EnvVars.masterEnvVars));
            return result;
        }
    }
}
