package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction implements EnvironmentContributingAction {

    public EnvInjectPluginAction(AbstractBuild build, Map<String, String> envMap) {
        super(build, envMap);
    }

    public Object getTarget() {
        return new EnvInjectVarList(envMap);
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        EnvInjectVarList varList = (EnvInjectVarList) getTarget();
        env.putAll(varList.getEnvMap());
    }
}
