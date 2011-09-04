package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectScriptExecutorService;
import org.jenkinsci.plugins.envinject.service.PropertiesVariablesRetriever;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
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

        final Map<String, String> resultVariables = new HashMap<String, String>();
        if (isEnvInjectJobPropertyActive(build)) {
            try {

                EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(build);
                assert envInjectJobProperty != null;
                EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
                assert envInjectJobProperty != null && envInjectJobProperty.isOn();

                //Ad Jenkins System variables
                if (envInjectJobProperty.isKeepJenkinsSystemVariables()) {
                    resultVariables.putAll(build.getEnvironment(new LogTaskListener(LOG, Level.ALL)));
                }

                //Add build variables (such as parameter variables).
                if (envInjectJobProperty.isKeepBuildVariables()) {
                    resultVariables.putAll(getAndAddBuildVariables(build));
                }

                //Build a properties object with all information
                final Map<String, String> envMap = getEnvVarsFromInfoObject(info, resultVariables, launcher, listener);
                resultVariables.putAll(envMap);

                //Resolves vars each other
                EnvVars.resolve(resultVariables);

                //Add a display action
                FilePath rootPath = getNodeRootPath();
                if (rootPath != null) {
                    new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, resultVariables);
                }
            } catch (EnvInjectException envEx) {
                listener.getLogger().println("SEVERE ERROR occurs: " + envEx.getMessage());
                throw new Run.RunnerAbortedException();
            } catch (Throwable throwable) {
                listener.getLogger().println("SEVERE ERROR occurs: " + throwable.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(resultVariables);
            }
        };
    }

    private Map<String, String> getEnvVarsFromInfoObject(final EnvInjectJobPropertyInfo info, final Map<String, String> currentEnvVars, final Launcher launcher, BuildListener listener) throws Throwable {

        final Map<String, String> resultMap = new HashMap<String, String>();

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        FilePath rootPath = getNodeRootPath();
        if (rootPath != null) {

            //Get env vars from properties
            resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(info, currentEnvVars, logger)));

            //Execute script info
            EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, currentEnvVars, rootPath, launcher, logger);
            scriptExecutorService.executeScriptFromInfoObject();
        }

        return resultMap;
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

    private Map<String, String> getAndAddBuildVariables(AbstractBuild build) {
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
