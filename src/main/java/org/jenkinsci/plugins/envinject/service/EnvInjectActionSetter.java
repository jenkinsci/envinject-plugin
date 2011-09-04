package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.envinject.EnvInjectAction;
import org.jenkinsci.plugins.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
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
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        if (envInjectAction != null) {
            envInjectAction.overrideAll(envMap);
        } else {
            envInjectAction = new EnvInjectAction(rootPath.act(new Callable<Map<String, String>, EnvInjectException>() {
                public Map<String, String> call() throws EnvInjectException {
                    return EnvVars.masterEnvVars;
                }
            }));
            envInjectAction.overrideAll(envMap);
            build.addAction(envInjectAction);
        }
    }
}
