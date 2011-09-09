package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectScriptExecutorService {

    private EnvInjectJobPropertyInfo info;

    private Map<String, String> currentEnvVars;

    private FilePath rootScriptExecutionPath;

    private Launcher launcher;

    private EnvInjectLogger logger;

    public EnvInjectScriptExecutorService(EnvInjectJobPropertyInfo info, Map<String, String> currentEnvVars, FilePath rootScriptExecutionPath, Launcher launcher, EnvInjectLogger logger) {
        this.info = info;
        this.currentEnvVars = currentEnvVars;
        this.rootScriptExecutionPath = rootScriptExecutionPath;
        this.launcher = launcher;
        this.logger = logger;
    }

    public void executeScriptFromInfoObject() throws EnvInjectException {

        //Process the script file path
        if (info.getScriptFilePath() != null) {
            String scriptFilePathResolved = Util.replaceMacro(info.getScriptFilePath(), currentEnvVars);
            String scriptFilePathNormalized = scriptFilePathResolved.replace("\\", "/");
            executeScriptPath(scriptFilePathNormalized);
        }

        //Process the script content
        if (info.getScriptContent() != null) {
            String scriptResolved = Util.replaceMacro(info.getScriptContent(), currentEnvVars);
            executeScriptContent(scriptResolved);
        }
    }


    private void executeScriptPath(String scriptFilePath) throws EnvInjectException {
        try {
            FilePath f = new FilePath(rootScriptExecutionPath, scriptFilePath);
            if (f.exists()) {
                launcher.getListener().getLogger().println(String.format("Executing '%s' script.", scriptFilePath));
                int cmdCode = launcher.launch().cmds(new File(scriptFilePath)).stdout(launcher.getListener()).pwd(rootScriptExecutionPath).join();
                if (cmdCode != 0) {
                    logger.info(String.format("The exit code is '%s'. Fail the build.", cmdCode));
                }
            } else {
                String message = String.format("Can't load the file '%s'. It doesn't exist.", f.getRemote());
                logger.error(message);
                throw new EnvInjectException(message);
            }
        } catch (Throwable e) {
            throw new EnvInjectException("Error occurs on execution script file path.", e);
        }
    }

    private void executeScriptContent(String scriptContent) throws EnvInjectException {

        try {

            CommandInterpreter batchRunner;
            if (launcher.isUnix()) {
                batchRunner = new Shell(scriptContent);
            } else {
                batchRunner = new BatchFile(scriptContent);
            }

            FilePath tmpFile = batchRunner.createScriptFile(rootScriptExecutionPath);
            logger.info(String.format("Executing the script: \n %s", scriptContent));
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(launcher.getListener()).pwd(rootScriptExecutionPath).join();
            if (cmdCode != 0) {
                String message = String.format("The exit code is '%s'. Fail the build.", cmdCode);
                logger.error(message);
                throw new EnvInjectException(message);
            }

        } catch (IOException ioe) {
            throw new EnvInjectException("Error occurs on execution script file path", ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException("Error occurs on execution script file path", ie);
        }
    }

}
