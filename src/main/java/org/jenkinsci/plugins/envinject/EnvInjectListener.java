package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {

    private Map<String, String> computeEnvVarsFromInfoObject(final EnvInjectJobPropertyInfo info, AbstractBuild build, Launcher launcher, final BuildListener listener) throws Throwable {

        final Map<String, String> envMap = new HashMap<String, String>();

        Computer computer = Computer.currentComputer();
        FilePath rootPath = computer.getNode().getRootPath();

        //Get env vars from properties
        envMap.putAll(computer.getNode().getRootPath().act(new EnvInjectGetEnvVarsFromPropertiesVariables(info, listener)));

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

        return envMap;
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        @SuppressWarnings("unchecked")
        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) build.getProject().getProperty(EnvInjectJobProperty.class);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                try {
                    //Build a properties object with all information
                    final Map<String, String> envMap = computeEnvVarsFromInfoObject(envInjectJobProperty.getInfo(), build, launcher, listener);

                    //Add system environment variables is needed
                    if (envInjectJobProperty.isKeepSystemVariables()) {
                        envMap.putAll(System.getenv());
                    }

                    //Resolves vars each other
                    EnvVars.resolve(envMap);

                    //Set the new computer variables
                    Computer.currentComputer().getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(envMap)));

                    //Add a display action
                    build.addAction(new EnvInjectAction(envMap));

                } catch (EnvInjectException envEx) {
                    listener.getLogger().println("SEVERE ERROR occurs: " + envEx.getMessage());
                    throw new Run.RunnerAbortedException();
                } catch (Throwable throwable) {
                    listener.getLogger().println("SEVERE ERROR occurs: " + throwable.getMessage());
                    throw new Run.RunnerAbortedException();
                }
            }
        }
        return new Environment() {
        };
    }

    @Override
    public void onCompleted(final Run run, final TaskListener listener) {

        @SuppressWarnings("unchecked")
        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) run.getParent().getProperty(EnvInjectJobProperty.class);
        if (envInjectJobProperty != null) {
            EnvInjectInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                try {
                    Computer.currentComputer().getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(System.getenv())));
                } catch (EnvInjectException e) {
                    run.setResult(Result.FAILURE);
                } catch (InterruptedException e) {
                    run.setResult(Result.FAILURE);
                } catch (IOException e) {
                    run.setResult(Result.FAILURE);
                }
            }
        }
    }
}
