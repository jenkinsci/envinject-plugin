package org.jenkinsci.plugins.envinject;

import hudson.*;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;
import hudson.tasks.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

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
public class EnvInjectWrapper extends BuildWrapper implements Serializable {

    private EnvInjectUIInfo info;

    public EnvInjectUIInfo getInfo() {
        return info;
    }

    public void setInfo(EnvInjectUIInfo info) {
        this.info = info;
    }


    @Override
    public Launcher decorateLauncher(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {

        Computer computer = Computer.currentComputer();
        FilePath rootPath = computer.getNode().getRootPath();

        //Compute new environment map
        EnvInjectLoadPropertiesVariables propertiesVariablesProcess = new EnvInjectLoadPropertiesVariables(info, listener);
        final Map<String, String> envMap = new HashMap<String, String>();
        try {

            //Process properties
            envMap.putAll(computer.getNode().getRootPath().act(propertiesVariablesProcess));

            //Process the script file path
            if (info.getScriptFilePath() != null) {
                boolean isFileExist = rootPath.act(new Callable<Boolean, Throwable>() {
                    public Boolean call() throws Throwable {
                        File f = new File(info.getScriptFilePath());
                        if (!f.exists()) {
                            listener.getLogger().print(String.format("Can't load the file '%s'. It doesn't exist.", f.getPath()));
                            return false;
                        }
                        return true;
                    }
                });

                if (isFileExist) {
                    listener.getLogger().print(String.format("Executing '%s' script.", info.getScriptFilePath()));
                    int cmdCode = launcher.launch().cmds(new File(info.getScriptFilePath())).stdout(listener).pwd(rootPath).join();
                    if (cmdCode != 0) {
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
                listener.getLogger().print(String.format("Executing the script: \n %s", script));
                int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(listener).pwd(runScriptPath).join();
                if (cmdCode != 0) {
                    build.setResult(Result.FAILURE);
                }
            }

            //Process if keep System is needed
            if (info.isKeepSystemVariables()) {
                envMap.putAll(System.getenv());
            }

        } catch (Throwable e) {
            throw new Run.RunnerAbortedException();
        }
        EnvVars.resolve(envMap);


        //Reset the computer variables
        try {
            computer.getNode().getRootPath().act(new Callable<Void, Throwable>() {
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
        } catch (Throwable throwable) {
            throw new Run.RunnerAbortedException();
        }

        //Add an environment action
        build.addAction(new EnvInjectAction(envMap));

        return launcher;
    }


    @Extension
    @SuppressWarnings("unused")
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

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new EnvironmentImpl();
    }

    class EnvironmentImpl extends Environment {
        @Override
        public void buildEnvVars(Map<String, String> env) {
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.envinject_displayName();
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help.html";
        }

        @Override
        public EnvInjectWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            EnvInjectWrapper envInjectorWrapper = new EnvInjectWrapper();
            EnvInjectUIInfo info = req.bindParameters(EnvInjectUIInfo.class, "info.");
            envInjectorWrapper.setInfo(info);
            return envInjectorWrapper;
        }
    }

}
