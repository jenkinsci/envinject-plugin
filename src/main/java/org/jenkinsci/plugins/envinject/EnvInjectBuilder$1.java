package org.jenkinsci.plugins.envinject;

import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@SuppressWarnings("unused")
public class EnvInjectBuilder$1 extends EnvInjectAction {

    private transient String val$resultVariables;
    private transient String this$0;

    public EnvInjectBuilder$1(AbstractBuild build, Map<String, String> envMap) {
        super(build, envMap);
    }

}
