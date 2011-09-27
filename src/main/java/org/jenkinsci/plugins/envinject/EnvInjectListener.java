package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.envinject.service.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {

    private static Logger LOG = Logger.getLogger(EnvInjectListener.class.getName());

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        if (isEnvInjectJobPropertyActive(build)) {
            try {

                Map<String, String> variables = new LinkedHashMap<String, String>();

                EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(build);
                assert envInjectJobProperty != null;
                EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
                assert envInjectJobProperty != null && envInjectJobProperty.isOn();

                //Add Jenkins System variables
                if (envInjectJobProperty.isKeepJenkinsSystemVariables()) {
                    variables.putAll(build.getEnvironment(new LogTaskListener(LOG, Level.ALL)));
                }

                //Add build variables (such as parameter variables).
                if (envInjectJobProperty.isKeepBuildVariables()) {
                    variables.putAll(getBuildVariables(build));
                }

                final FilePath rootPath = getNodeRootPath();
                if (rootPath != null) {

                    //Build a properties object with all information
                    final Map<String, String> envMap = getEnvVarsFromProperties(rootPath, info, variables, launcher, listener);
                    variables.putAll(envMap);

                    // Retrieve triggered cause
                    if (info.isPopulateTriggerCause()) {
                        Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(build);
                        variables.putAll(triggerVariable);
                    }

                    //Resolves vars each other
                    EnvVars.resolve(variables);

                    //Remove unset variables
                    final Map<String, String> resultVariables = new EnvInjectEnvVarsUnset(logger).removeUnsetVars(variables);

                    //Add an action
                    new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, resultVariables);

                    //Execute script
                    executeScript(rootPath, info, resultVariables, launcher, listener);

                    return new Environment() {
                        @Override
                        public void buildEnvVars(Map<String, String> env) {
                            env.putAll(resultVariables);
                        }
                    };
                }

            } catch (EnvInjectException envEx) {
                logger.error("SEVERE ERROR occurs: " + envEx.getMessage());
                throw new Run.RunnerAbortedException();
            } catch (Throwable throwable) {
                logger.error("SEVERE ERROR occurs: " + throwable.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }


        return new Environment() {
        };
    }


    private Map<String, String> getEnvVarsFromProperties(FilePath rootPath, final EnvInjectJobPropertyInfo info, final Map<String, String> currentEnvVars, final Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        //Get env vars from properties
        resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(info, currentEnvVars, logger)));
        return resultMap;
    }

    private static void executeScript(FilePath rootPath, final EnvInjectJobPropertyInfo info, final Map<String, String> currentEnvVars, final Launcher launcher, BuildListener listener) throws EnvInjectException {
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, currentEnvVars, rootPath, launcher, logger);
        scriptExecutorService.executeScriptFromInfoObject();
    }

    private Node getNode() {
        Computer computer = Computer.currentComputer();
        return computer.getNode();
    }

    private FilePath getNodeRootPath() {
        Node node = getNode();
        if (node != null) {
            return node.getRootPath();
        }
        return null;
    }

    private Map<String, String> getBuildVariables(AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();

        //Add build process variables
        result.putAll(build.getCharacteristicEnvVars());

        //Add build variables such as parameters, plugins contributions, ...
        result.putAll(build.getBuildVariables());

        //Add workspace variable
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            result.put("WORKSPACE", ws.getRemote());
        }
        return result;
    }

    private boolean isEnvInjectJobPropertyActive(Run run) {
        EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(run);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                return true;
            }
        }
        return false;
    }

    private EnvInjectJobProperty getEnvInjectJobProperty(Run run) {
        return (EnvInjectJobProperty) run.getParent().getProperty(EnvInjectJobProperty.class);
    }
}
