package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildVariableContributor;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectBuildVariableContributor extends BuildVariableContributor {

    @Override
    public void buildVariablesFor(AbstractBuild build, Map<String, String> variables) {
        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            variables.putAll(envInjectAction.getEnvMap());
        }
    }
}
