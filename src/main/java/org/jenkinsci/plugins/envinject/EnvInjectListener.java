package org.jenkinsci.plugins.envinject;

import hudson.*;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;
import org.jenkinsci.plugins.envinject.service.PropertiesVariablesRetriever;

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

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        @SuppressWarnings("unchecked")
        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) build.getProject().getProperty(EnvInjectJobProperty.class);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {

                Map<String, String> resultVariables = new HashMap<String, String>();

                try {

                    //Add system environment variables if needed
                    if (envInjectJobProperty.isKeepSystemVariables()) {
                        //The new envMap wins
                        resultVariables.putAll(System.getenv());
                    }

                    //Always beep build variables (such as parameter variables).
                    resultVariables.putAll(getAndAddBuildVariables(build));

                    //Build a properties object with all information
                    final Map<String, String> envMap = getEnvVarsFromInfoObject(info, resultVariables, launcher, listener);
                    resultVariables.putAll(envMap);

                    //Resolves vars each other
                    EnvVars.resolve(resultVariables);

                    //Set the new computer variables
                    Computer.currentComputer().getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(resultVariables)));

                    //Add a display action
                    build.addAction(new EnvInjectAction(resultVariables));

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

    private Map<String, String> getAndAddBuildVariables(AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();
        //Add build variables such as parameters
        result.putAll(build.getBuildVariables());
        //Add workspace variable
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            result.put("WORKSPACE", ws.getRemote());
        }
        return result;
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

    private Map<String, String> getEnvVarsFromInfoObject(EnvInjectJobPropertyInfo info,
                                                         Map<String, String> currentEnvVars,
                                                         Launcher launcher,
                                                         final BuildListener listener) throws Throwable {

        final Map<String, String> resultMap = new HashMap<String, String>();

        Computer computer = Computer.currentComputer();
        Node node = computer.getNode();
        if (node != null) {
            FilePath rootPath = node.getRootPath();
            if (rootPath != null) {

                //Get env vars from properties
                resultMap.putAll(node.getRootPath().act(new PropertiesVariablesRetriever(info, listener, currentEnvVars)));

                //Process the script file path
                if (info.getScriptFilePath() != null) {
                    String scriptFilePathResolved = Util.replaceMacro(info.getScriptFilePath(), currentEnvVars);
                    String scriptFilePathNormalized = scriptFilePathResolved.replace("\\", "/");
                    executeScriptPath(scriptFilePathNormalized, launcher, listener, rootPath);
                }

                //Process the script content
                if (info.getScriptContent() != null) {
                    String scriptResolved = Util.replaceMacro(info.getScriptContent(), currentEnvVars);
                    executeScriptContent(scriptResolved, launcher, listener, rootPath);
                }
            }
        }
        return resultMap;
    }


    private void executeScriptPath(final String scriptFilePath,
                                   Launcher launcher,
                                   final BuildListener listener,
                                   FilePath rootPath) throws EnvInjectException {
        try {
            boolean isFileExist = rootPath.act(new Callable<Boolean, Throwable>() {
                public Boolean call() throws Throwable {
                    File f = new File(scriptFilePath);
                    if (!f.exists()) {
                        listener.getLogger().println(String.format("Can't load the file '%s'. It doesn't exist.", f.getPath()));
                        return false;
                    }
                    return true;
                }
            });

            if (isFileExist) {
                listener.getLogger().println(String.format("Executing '%s' script.", scriptFilePath));
                int cmdCode = launcher.launch().cmds(new File(scriptFilePath)).stdout(listener).pwd(rootPath).join();
                if (cmdCode != 0) {
                    listener.getLogger().println(String.format("The exit code is '%s'. Fail the build.", cmdCode));
                }
            }
        } catch (Throwable e) {
            throw new EnvInjectException("Error occurs on execution script file path", e);
        }
    }

    private void executeScriptContent(
            String scriptContent,
            Launcher launcher,
            BuildListener listener,
            FilePath rootPath) throws EnvInjectException {

        try {

            CommandInterpreter batchRunner;
            if (launcher.isUnix()) {
                batchRunner = new Shell(scriptContent);
            } else {
                batchRunner = new BatchFile(scriptContent);
            }

            FilePath runScriptPath = new FilePath(rootPath, "tmp");
            runScriptPath.mkdirs();

            FilePath tmpFile = batchRunner.createScriptFile(runScriptPath);
            listener.getLogger().println(String.format("Executing the script: \n %s", scriptContent));
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(listener).pwd(runScriptPath).join();
            if (cmdCode != 0) {
                listener.getLogger().println(String.format("The exit code is '%s'. Fail the build.", cmdCode));
            }

        } catch (IOException ioe) {
            throw new EnvInjectException("Error occurs on execution script file path", ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException("Error occurs on execution script file path", ie);
        }
    }

}
