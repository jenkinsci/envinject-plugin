package org.jenkinsci.plugins.envinject;

import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 */
@SuppressWarnings("unused")
public class EnvInjectBuilder$1 extends EnvInjectAction {

    private transient String val$resultVariables;
    private transient String this$0;

    public EnvInjectBuilder$1(@Nonnull AbstractBuild build, @CheckForNull Map<String, String> envMap) {
        super(build, envMap);
    }

}
