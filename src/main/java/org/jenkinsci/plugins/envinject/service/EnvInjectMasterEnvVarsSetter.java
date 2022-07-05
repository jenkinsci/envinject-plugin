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

    private Field getModifiers() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
        Field modifiers = null;
        for (Field each : fields) {
            if ("modifiers".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        return modifiers;
    }

    @Override
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
            Field modifiersField = getModifiers();
            modifiersField.setAccessible(true);
            modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
            masterEnvVarsFiled.set(null, enVars);
        } catch (IllegalAccessException iae) {
            throw new EnvInjectException(iae);
        } catch (NoSuchFieldException nsfe) {
            throw new EnvInjectException(nsfe);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}
