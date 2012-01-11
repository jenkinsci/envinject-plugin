package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.Main;
import hudson.Platform;
import hudson.remoting.Callable;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectMasterEnvVarsSetter implements Callable<Void, EnvInjectException> {

    private EnvVars enVars;

    public EnvInjectMasterEnvVarsSetter(EnvVars enVars) {
        this.enVars = enVars;
    }

    public Void call() throws EnvInjectException {
        try {
            Field platformField = EnvVars.class.getDeclaredField("platform");
            platformField.setAccessible(true);
            platformField.set(enVars, Platform.current());
            if (Main.isUnitTest || Main.isDevelopmentMode) {
                enVars.remove("MAVEN_OPTS");
            }
            Field masterEnvVarsFiled = EnvVars.class.getDeclaredField("masterEnvVars");
            masterEnvVarsFiled.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
            masterEnvVarsFiled.set(null, enVars);
        } catch (IllegalAccessException iae) {
            throw new EnvInjectException(iae);
        } catch (NoSuchFieldException nsfe) {
            throw new EnvInjectException(nsfe);
        }

        return null;
    }

}
