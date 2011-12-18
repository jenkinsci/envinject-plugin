package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.envinject.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectVariableGetter {

    private static Logger LOG = Logger.getLogger(EnvInjectVariableGetter.class.getName());

    public Map<String, String> getJenkinsSystemVariablesCurrentNode(AbstractBuild build) throws IOException, InterruptedException {

        Map<String, String> result = new TreeMap<String, String>();

        //Environment variables
        result.putAll(build.getEnvironment(new LogTaskListener(LOG, Level.ALL)));

        //Global properties
        for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                EnvironmentVariablesNodeProperty environmentVariablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                result.putAll(environmentVariablesNodeProperty.getEnvVars());
            }
        }

        //Node properties
        Computer computer = Computer.currentComputer();
        if (computer != null) {
            Node node = computer.getNode();
            if (node != null) {
                for (NodeProperty<?> nodeProperty : node.getNodeProperties()) {
                    if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        EnvironmentVariablesNodeProperty environmentVariablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                        result.putAll(environmentVariablesNodeProperty.getEnvVars());
                    }
                }
            }
        }

        return result;
    }


    public Map<String, String> getBuildVariables(AbstractBuild build, EnvInjectLogger logger) throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        //Add build process variables
        result.putAll(build.getCharacteristicEnvVars());

        //Add build variables such as parameters, plugins contributions, ...
        result.putAll(build.getBuildVariables());

        //Add workspace variable
        String workspace = getWorkspaceWithCreation(build, logger);
        if (workspace != null) {
            result.put("WORKSPACE", workspace);
        }

        return result;
    }

    private String getWorkspaceWithCreation(AbstractBuild build, EnvInjectLogger logger) throws EnvInjectException {
        try {
            Node node = build.getBuiltOn();
            if (node != null) {
                Job job = build.getParent();
                if (job instanceof TopLevelItem) {
                    FilePath workspace = decideWorkspace(build, (TopLevelItem) job, node, logger.getListener());
                    workspace.mkdirs();
                    return workspace.getRemote();
                }
            }
            return null;
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        } catch (IOException ie) {
            throw new EnvInjectException(ie);
        }
    }

    private FilePath decideWorkspace(AbstractBuild build, TopLevelItem item, Node n, TaskListener listener) throws InterruptedException, IOException {
        if (item instanceof AbstractProject) {
            String customWorkspace = ((AbstractProject) item).getCustomWorkspace();
            if (customWorkspace != null) {
                return n.getRootPath().child(build.getEnvironment(listener).expand(customWorkspace));
            }
        }
        return n.getWorkspaceFor(item);
    }

    public boolean isEnvInjectJobPropertyActive(Job job) {
        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) job.getProperty(EnvInjectJobProperty.class);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                return true;
            }
        }
        return false;
    }

    public EnvInjectJobProperty getEnvInjectJobProperty(Job project) {
        return (EnvInjectJobProperty) project.getProperty(EnvInjectJobProperty.class);
    }

    public Map<String, String> getPreviousEnvVars(AbstractBuild build, EnvInjectLogger logger) throws IOException, InterruptedException, EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();
        if (isEnvInjectActivated(build)) {
            result.putAll(getCurrentInjectedEnvVars(build));
        } else {
            result.putAll(getJenkinsSystemVariablesCurrentNode(build));
            result.putAll(getBuildVariables(build, logger));
        }
        return result;
    }

    private boolean isEnvInjectActivated(AbstractBuild build) {
        if (build instanceof MatrixRun) {
            return (((MatrixRun) build).getParentBuild().getAction(EnvInjectAction.class)) != null;
        } else {
            return build.getAction(EnvInjectAction.class) != null;
        }
    }

    public Map<String, String> getCurrentInjectedEnvVars(AbstractBuild<?, ?> build) {
        EnvInjectAction envInjectAction = getEnvInjectAction(build);
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (envInjectAction == null) {
            return result;
        } else {
            result.putAll(envInjectAction.getEnvMap());
            return result;
        }
    }

    private EnvInjectAction getEnvInjectAction(AbstractBuild<?, ?> build) {
        EnvInjectAction envInjectAction;
        if (build instanceof MatrixRun) {
            envInjectAction = ((MatrixRun) build).getParentBuild().getAction(EnvInjectAction.class);
        } else {
            envInjectAction = build.getAction(EnvInjectAction.class);
        }
        return envInjectAction;
    }

}
