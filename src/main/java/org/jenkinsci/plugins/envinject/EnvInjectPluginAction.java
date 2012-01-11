package org.jenkinsci.plugins.envinject;

import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction {

    public EnvInjectPluginAction(AbstractBuild build, Map<String, String> envMap) {
        super(build, envMap);
    }

    public Object getTarget() {
        return new EnvInjectVarList(envMap);
    }
}
