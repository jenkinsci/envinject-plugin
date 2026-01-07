package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.Main;
import hudson.Platform;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectGlobalStorage;

import java.lang.reflect.Field;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectMasterEnvVarsSetter extends MasterToSlaveCallable<Void, EnvInjectException> {

    private @NonNull EnvVars enVars;

    public EnvInjectMasterEnvVarsSetter(@NonNull EnvVars enVars) {
        this.enVars = enVars;
    }

    @Override
    public Void call() throws EnvInjectException {
        // Check if there's nothing to update
        EnvVars currentMerged = EnvInjectGlobalStorage.getMergedVars(EnvVars.masterEnvVars);
        if (currentMerged.equals(enVars)) {
            // Nothing to update
            return null;
        } else if (currentMerged.keySet().equals(enVars.keySet())) {
           /*
            * Per the Javadoc, merely changing the value associated with an existing key is not a structural
            * modification and thus does not require synchronization.
            * We update the values while preserving the keyset structure.
            */
           EnvInjectGlobalStorage.updateInjectedVars(enVars);
           return null;
        }

        try {
            // Set platform field on the enVars object
            Field platformField = EnvVars.class.getDeclaredField("platform");
            platformField.setAccessible(true);
            platformField.set(enVars, Platform.current());
            if (Main.isUnitTest || Main.isDevelopmentMode) {
                enVars.remove("MAVEN_OPTS");
            }
            // Store in global storage instead of using reflection to modify masterEnvVars
            EnvInjectGlobalStorage.setInjectedVars(enVars);
        } catch (IllegalAccessException | NoSuchFieldException iae) {
            throw new EnvInjectException(iae);
        }

        return null;
    }

}
