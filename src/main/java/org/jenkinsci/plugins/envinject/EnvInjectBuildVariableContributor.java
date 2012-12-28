package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildVariableContributor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */

/**
 * Overriding job parameters with environment variables populated by EnvInject plugin
 */
@Extension
public class EnvInjectBuildVariableContributor extends BuildVariableContributor {

    @Override
    public void buildVariablesFor(AbstractBuild build, Map<String, String> variables) {
        ParametersAction parameters = build.getAction(ParametersAction.class);
        //Only for a parameterized job
        if (parameters != null) {
            EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
            if (envInjectAction != null) {
                for (ParameterValue p : parameters) {
                    String key = p.getName();
                    Map<String, String> injectedEnvVars = envInjectAction.getEnvMap();
                    if (injectedEnvVars.containsKey(key)) {
                        variables.put(key, injectedEnvVars.get(key));
                    }
                }
            }
        }
    }
}
