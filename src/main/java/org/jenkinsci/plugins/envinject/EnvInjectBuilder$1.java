package org.jenkinsci.plugins.envinject;

import hudson.RestrictedSince;
import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 * @deprecated replaced by {@link EnvInjectBuilder}
 */
@SuppressWarnings("unused")
@Restricted(NoExternalUse.class)
@RestrictedSince("2.1")
@Deprecated
public class EnvInjectBuilder$1 extends EnvInjectAction {

    private transient String val$resultVariables;
    private transient String this$0;

    public EnvInjectBuilder$1(@Nonnull AbstractBuild build, @CheckForNull Map<String, String> envMap) {
        super(build, envMap);
    }
    
    public EnvInjectBuilder$1(@CheckForNull Map<String, String> envMap) {
        super(envMap);
    }
}
