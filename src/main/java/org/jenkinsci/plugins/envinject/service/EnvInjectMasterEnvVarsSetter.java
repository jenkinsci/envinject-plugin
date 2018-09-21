package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.Main;
import hudson.Platform;
import hudson.util.VersionNumber;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectMasterEnvVarsSetter extends MasterToSlaveCallable<Void, EnvInjectException> {

    private @Nonnull EnvVars enVars;
    
    // TODO: this may not be the version of core that gets the fix allowing us to
    // use safe reflection to set the Platform. The exact version needs to be updated 
    // after that version is released.
    private static VersionNumber jenkinsVersionWithSetPlatform = new VersionNumber("2.139");

    public EnvInjectMasterEnvVarsSetter(@Nonnull EnvVars enVars) {
        this.enVars = enVars;
    }

    @Override
    public Void call() throws EnvInjectException {
        try {
            if (Jenkins.getVersion().isOlderThan(jenkinsVersionWithSetPlatform)) {
                Field platformField = EnvVars.class.getDeclaredField("platform");

                platformField.setAccessible(true);
                platformField.set(enVars, Platform.current());                
            } else {
                Method method = EnvVars.class.getDeclaredMethod("setPlatform", Platform.class);
                method.invoke(enVars, Platform.current());
            }
            if (Main.isUnitTest || Main.isDevelopmentMode) {
                enVars.remove("MAVEN_OPTS");
            }
            
            EnvVars.masterEnvVars.clear();
            EnvVars.masterEnvVars.putAll(enVars); 
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException exception) {
            throw new EnvInjectException(exception);
        }

        return null;
    }

}
