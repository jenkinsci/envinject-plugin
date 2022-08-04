package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import java.util.NavigableMap;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectMasterEnvVarsSetter extends MasterToSlaveCallable<Void, EnvInjectException> {

    private @NonNull NavigableMap<String, String> enVars;

    public EnvInjectMasterEnvVarsSetter(@NonNull NavigableMap<String, String> enVars) {
        this.enVars = enVars;
    }

    @Override
    public Void call() {
        EnvVars.masterEnvVars.putAll(enVars);
        return null;
    }
}
