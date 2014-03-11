package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.model.*;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.jenkinsci.plugins.envinject.service.EnvironmentVariablesNodeLoader;

import java.util.HashMap;
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

            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);
            if (envInjectJobProperty == null) {
                // Don't override anything if envinject isn't enabled on this job
                return;
            }

            if (!envInjectJobProperty.isOverrideBuildParameters()) return;

            //Gather global variables for the current node
            EnvironmentVariablesNodeLoader environmentVariablesNodeLoader = new EnvironmentVariablesNodeLoader();
            Map<String, String> nodeEnvVars = new HashMap<String, String>();
            try {
                nodeEnvVars = environmentVariablesNodeLoader.gatherEnvironmentVariablesNode(build, build.getBuiltOn(), new EnvInjectLogger(TaskListener.NULL));
            } catch (EnvInjectException e) {
                e.printStackTrace();
            }

            EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
            if (envInjectAction != null) {
                for (ParameterValue p : parameters) {
                    String key = p.getName();
                    Map<String, String> injectedEnvVars = envInjectAction.getEnvMap();
                    if (injectedEnvVars == null) {
                        return;
                    }

                    //---INPUTS
                    //GLOBAL envVars, parameters envVars, injectedEnvVars (with global)


                    //--CLASSIC USE CASES
                    //CASE1 :  var in global and in parameter
                    // si non job --> parameter win
                    // si dans job --> ceux du job

                    //CASE2: var in global and not in parameter --> nothing


                    // key in parameter, in job (not in global and already injected)
                    // --> override
                    if (injectedEnvVars.containsKey(key) && !nodeEnvVars.containsKey(key)) {
                        variables.put(key, injectedEnvVars.get(key));
                    }
                }
            }
        }
    }
}
