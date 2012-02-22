package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.remoting.Callable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionSetter implements Serializable {

    private FilePath rootPath;

    public EnvInjectActionSetter(FilePath rootPath) {
        this.rootPath = rootPath;
    }

    public void addEnvVarsToEnvInjectBuildAction(AbstractBuild<?, ?> build, Map<String, String> envMap) throws EnvInjectException, IOException, InterruptedException {
        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            envInjectAction.overrideAll(envMap);
        } else {
            if (rootPath != null) {
                envInjectAction = new EnvInjectPluginAction(build, rootPath.act(new Callable<Map<String, String>, EnvInjectException>() {
                    public Map<String, String> call() throws EnvInjectException {
                        HashMap<String, String> result = new HashMap<String, String>();
                        result.putAll(EnvVars.masterEnvVars);
                        return result;
                    }
                }));
                envInjectAction.overrideAll(envMap);
                build.addAction(envInjectAction);
            }
        }
    }
}
