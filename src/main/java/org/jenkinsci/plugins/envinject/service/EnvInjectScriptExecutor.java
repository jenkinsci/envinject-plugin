package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.IOException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectScriptExecutor {

    private Launcher launcher;

    private EnvInjectLogger logger;

    public EnvInjectScriptExecutor(Launcher launcher, EnvInjectLogger logger) {
        this.launcher = launcher;
        this.logger = logger;
    }

    public int executeScriptSection(FilePath scriptExecutionRoot,
                                    String scriptFilePath,
                                    String scriptContent,
                                    Map<String, String> scriptPathExecutionEnvVars,
                                    Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {

        //Process the script file path
        if (scriptFilePath != null) {
            String scriptFilePathResolved = Util.replaceMacro(scriptFilePath, scriptPathExecutionEnvVars);
            String scriptFilePathNormalized = scriptFilePathResolved.replace("\\", "/");
            int resultCode = executeScriptPath(scriptExecutionRoot, scriptFilePathNormalized, scriptExecutionEnvVars);
            if (resultCode != 0) {
                return resultCode;
            }

        }

        //Process the script content
        if (scriptContent != null) {
            int resultCode = executeScriptContent(scriptExecutionRoot, scriptContent, scriptExecutionEnvVars);
            if (resultCode != 0) {
                return resultCode;
            }

        }


        return 0;
    }


    private int executeScriptPath(FilePath scriptExecutionRoot, String scriptFilePath, Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {
        try {
            launcher.getListener().getLogger().println(String.format("Executing '%s'.", scriptFilePath));
            ArgumentListBuilder cmds = new ArgumentListBuilder();
            cmds.addTokenized(scriptFilePath);
            int cmdCode = launcher.launch().cmds(cmds).stdout(launcher.getListener()).envs(scriptExecutionEnvVars).pwd(scriptExecutionRoot).join();
            if (cmdCode != 0) {
                logger.info(String.format("Script executed. The exit code is %s.", cmdCode));
            } else {
                logger.info("Script executed successfully.");
            }
            return cmdCode;
        } catch (Throwable e) {
            throw new EnvInjectException("Error occurs on execution script file path.", e);
        }
    }

    private int executeScriptContent(FilePath scriptExecutionRoot, String scriptContent, Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {

        try {

            CommandInterpreter batchRunner;
            if (launcher.isUnix()) {
                batchRunner = new Shell(scriptContent);
            } else {
                batchRunner = new BatchFile(scriptContent);
            }

            FilePath tmpFile = batchRunner.createScriptFile(scriptExecutionRoot);
            logger.info(String.format("Executing and processing the following script content: \n%s\n", scriptContent));
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(launcher.getListener()).envs(scriptExecutionEnvVars).pwd(scriptExecutionRoot).join();
            if (cmdCode != 0) {
                logger.info(String.format("Script executed. The exit code is %s.", cmdCode));
            } else {
                logger.info("Script executed successfully.");
            }
            return cmdCode;

        } catch (IOException ioe) {
            throw new EnvInjectException("Error occurs on execution script file path", ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException("Error occurs on execution script file path", ie);
        }
    }

}
