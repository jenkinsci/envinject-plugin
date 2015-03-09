package org.jenkinsci.plugins.envinject;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction implements EnvironmentContributingAction {

    public EnvInjectPluginAction(AbstractBuild build, Map<String, String> envMap) {
        super(build, envMap);
    }

    public Object getTarget() {
        return new EnvInjectVarList(Maps.transformEntries(envMap,
                new Maps.EntryTransformer<String, String, String>() {
                    public String transformEntry(String key, String value) {
                        return getSensibleVariables() != null && getSensibleVariables().contains(key) ? "********" : value;
                    }
                }));
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        EnvInjectVarList varList = (EnvInjectVarList) getTarget();
        Map<String, String> envMap = varList.getEnvMap();
        if (envMap != null) {
            env.putAll(envMap);
        }
    }
}
