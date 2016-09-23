package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionSetter implements Serializable {

    @CheckForNull
    private FilePath rootPath;

    public EnvInjectActionSetter(@CheckForNull FilePath rootPath) {
        this.rootPath = rootPath;
    }

    public void addEnvVarsToEnvInjectBuildAction(@Nonnull AbstractBuild<?, ?> build, @CheckForNull Map<String, String> envMap) 
            throws EnvInjectException, IOException, InterruptedException {

        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            envInjectAction.overrideAll(build.getSensitiveBuildVariables(), envMap);
        } else {
            if (rootPath != null) {
                envInjectAction = new EnvInjectPluginAction(build, rootPath.act(new MasterToSlaveCallable<Map<String, String>, EnvInjectException>() {
                    public Map<String, String> call() throws EnvInjectException {
                        HashMap<String, String> result = new HashMap<String, String>();
                        result.putAll(EnvVars.masterEnvVars);
                        return result;
                    }
                }));
                envInjectAction.overrideAll(build.getSensitiveBuildVariables(), envMap);
                build.addAction(envInjectAction);
            }
        }
    }
}
