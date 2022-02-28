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
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectScriptExecutor {

    @NonNull
    private final Launcher launcher;

    @NonNull
    private final EnvInjectLogger logger;

    public EnvInjectScriptExecutor(@NonNull Launcher launcher, @NonNull EnvInjectLogger logger) {
        this.launcher = launcher;
        this.logger = logger;
    }

    public int executeScriptSection(@CheckForNull FilePath scriptExecutionRoot,
                                    @CheckForNull String scriptFilePath,
                                    @CheckForNull String scriptContent,
                                    @NonNull Map<String, String> scriptPathExecutionEnvVars,
                                    @NonNull Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {

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

    // TODO: Null file path leads to NOP, maybe safe here
    private int executeScriptPath(
            @CheckForNull FilePath scriptExecutionRoot, @NonNull String scriptFilePath, 
            @NonNull Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {
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

    private int executeScriptContent(@NonNull FilePath scriptExecutionRoot, 
            @NonNull String scriptContent, @NonNull Map<String, String> scriptExecutionEnvVars) 
            throws EnvInjectException {

        try {

            CommandInterpreter batchRunner;
            if (launcher.isUnix()) {
                batchRunner = new Shell(scriptContent);
            } else {
                batchRunner = new BatchFile(scriptContent);
            }

            FilePath tmpFile = batchRunner.createScriptFile(scriptExecutionRoot);
            logger.info(String.format("Executing and processing the following script content: %n%s%n", scriptContent));
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(launcher.getListener())
                    .envs(scriptExecutionEnvVars).pwd(scriptExecutionRoot).join();
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
