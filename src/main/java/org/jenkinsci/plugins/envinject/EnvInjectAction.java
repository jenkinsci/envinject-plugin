package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Main;
import hudson.Platform;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements Action {


    @Extension
    public static class EnvInjectListener extends RunListener<Run> {

        @Override
        public void onCompleted(final Run run, final TaskListener listener) {

            if (run.getAction(EnvInjectAction.class) != null) {
                Computer computer = Computer.currentComputer();
                try {
                    computer.getNode().getRootPath().act(new Callable<Void, Throwable>() {
                        public Void call() throws Throwable {

                            //Prepare the new master variables
                            EnvVars vars = new EnvVars(System.getenv());
                            Field platformField = vars.getClass().getDeclaredField("platform");
                            platformField.setAccessible(true);
                            platformField.set(vars, Platform.current());
                            if (Main.isUnitTest || Main.isDevelopmentMode) {
                                vars.remove("MAVEN_OPTS");
                            }

                            //Set the new master variables
                            Field masterEnvVarsFiled = EnvVars.class.getDeclaredField("masterEnvVars");
                            masterEnvVarsFiled.setAccessible(true);
                            Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
                            masterEnvVarsFiled.set(null, vars);

                            return null;
                        }
                    });
                } catch (Throwable throwable) {
                    run.setResult(Result.FAILURE);
                }
            }
        }
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}
