package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.Main;
import hudson.Platform;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectMasterEnvVarsSetter extends MasterToSlaveCallable<Void, EnvInjectException> {

    private @NonNull EnvVars enVars;

    public EnvInjectMasterEnvVarsSetter(@NonNull EnvVars enVars) {
        this.enVars = enVars;
    }

    private Field getModifiers() throws NoSuchFieldException {
        try {
            // Try the direct approach first (works in Java 8-16)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            return modifiersField;
        } catch (NoSuchFieldException e) {
            // In Java 17+, the modifiers field might not be accessible
            // We'll handle this gracefully in the calling code
            throw new NoSuchFieldException("Unable to access modifiers field - may be running on Java 17+");
        }
    }

    @Override
    public Void call() throws EnvInjectException {
        if (EnvVars.masterEnvVars.equals(enVars)) {
            // Nothing to update
            return null;
        } else if (EnvVars.masterEnvVars.keySet().equals(enVars.keySet())) {
           /*
            * Per the Javadoc, merely changing the value associated with an existing key is not a structural
            * modification and thus does not require synchronization.
            */
           EnvVars.masterEnvVars.putAll(enVars);
           return null;
        }

        try {
            Field platformField = EnvVars.class.getDeclaredField("platform");
            platformField.setAccessible(true);
            platformField.set(enVars, Platform.current());
            if (Main.isUnitTest || Main.isDevelopmentMode) {
                enVars.remove("MAVEN_OPTS");
            }
            Field masterEnvVarsFiled = EnvVars.class.getDeclaredField("masterEnvVars");
            masterEnvVarsFiled.setAccessible(true);
            
            try {
                Field modifiersField = getModifiers();
                modifiersField.setAccessible(true);
                modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
                masterEnvVarsFiled.set(null, enVars);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // In Java 17+, modifying final fields via reflection is restricted
                // Try alternative approach: use putAll to update the existing EnvVars instance
                // instead of replacing the static field entirely
                synchronized (EnvVars.masterEnvVars) {
                    EnvVars.masterEnvVars.clear();
                    EnvVars.masterEnvVars.putAll(enVars);
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException iae) {
            throw new EnvInjectException(iae);
        }

        return null;
    }

}
