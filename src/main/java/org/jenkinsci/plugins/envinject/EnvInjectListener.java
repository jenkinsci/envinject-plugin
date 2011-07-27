package org.jenkinsci.plugins.envinject;

import hudson.*;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {

    private Map<String, String> computeEnvVars(final EnvInjectUIInfo info, AbstractBuild build, Launcher launcher, final BuildListener listener) throws Throwable {

        final Map<String, String> envMap = new HashMap<String, String>();

        Computer computer = Computer.currentComputer();
        FilePath rootPath = computer.getNode().getRootPath();

        //Compute new environment map
        EnvInjectLoadPropertiesVariables propertiesVariablesProcess = new EnvInjectLoadPropertiesVariables(info, listener);

        //Process properties
        envMap.putAll(computer.getNode().getRootPath().act(propertiesVariablesProcess));

        //Process the script file path
        if (info.getScriptFilePath() != null) {
            boolean isFileExist = rootPath.act(new Callable<Boolean, Throwable>() {
                public Boolean call() throws Throwable {
                    File f = new File(info.getScriptFilePath());
                    if (!f.exists()) {
                        listener.getLogger().println(String.format("Can't load the file '%s'. It doesn't exist.", f.getPath()));
                        return false;
                    }
                    return true;
                }
            });

            if (isFileExist) {
                listener.getLogger().println(String.format("Executing '%s' script.", info.getScriptFilePath()));
                int cmdCode = launcher.launch().cmds(new File(info.getScriptFilePath())).stdout(listener).pwd(rootPath).join();
                if (cmdCode != 0) {
                    listener.getLogger().println(String.format("The exit code is '%s'. Fail the build.", cmdCode));
                    build.setResult(Result.FAILURE);
                }
            }
        }

        //Process the script content
        if (info.getScriptContent() != null) {
            CommandInterpreter batchRunner;
            String script = info.getPropertiesContent();

            if (launcher.isUnix()) {
                batchRunner = new Shell(script);
            } else {
                batchRunner = new BatchFile(script);
            }

            FilePath runScriptPath = new FilePath(rootPath, "tmp");
            runScriptPath.mkdirs();

            FilePath tmpFile = batchRunner.createScriptFile(runScriptPath);
            listener.getLogger().println(String.format("Executing the script: \n %s", script));
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(listener).pwd(runScriptPath).join();
            if (cmdCode != 0) {
                listener.getLogger().println(String.format("The exit code is '%s'. Fail the build.", cmdCode));
                build.setResult(Result.FAILURE);
            }
        }

        //Process if keep System is needed
        if (info.isKeepSystemVariables()) {
            envMap.putAll(System.getenv());
        }

        return envMap;
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) build.getProject().getProperty(EnvInjectJobProperty.class);
        EnvInjectUIInfo info = envInjectJobProperty.getInfo();
        if (envInjectJobProperty != null) {
            if (info != null && info.isOn()) {

                try {
                    final Map<String, String> envMap = computeEnvVars(envInjectJobProperty.getInfo(), build, launcher, listener);

                    EnvVars.resolve(envMap);

                    //Reset the computer variables
                    Computer.currentComputer().getNode().getRootPath().act(new Callable<Void, Throwable>() {
                        public Void call() throws Throwable {
                            Field masterEnvVarsFiled = EnvVars.class.getDeclaredField("masterEnvVars");
                            masterEnvVarsFiled.setAccessible(true);
                            Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
                            masterEnvVarsFiled.set(null, envMap);

                            return null;
                        }
                    });

                    //Add a display action
                    build.addAction(new EnvInjectAction(envMap));

                } catch (Throwable e) {
                    listener.getLogger().println("SEVERE ERROR occurs: " + e.getMessage());
                    throw new Run.RunnerAbortedException();
                }
            }
        }
        return new Environment() {
        };
    }

    @Override
    public void onCompleted(final Run run, final TaskListener listener) {

        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) run.getParent().getProperty(EnvInjectJobProperty.class);
        EnvInjectUIInfo info = envInjectJobProperty.getInfo();
        if (envInjectJobProperty != null) {
            if (info != null && info.isOn()) {
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
}
