package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.*;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.envinject.EnvInjectAction;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;

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


    public Map<String, String> getBuildVariables(AbstractBuild build) {
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

    public boolean isEnvInjectJobPropertyActive(Job job) {
        EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(job);
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

    public Map<String, String> getPreviousEnvVars(AbstractBuild build) throws IOException, InterruptedException {
        Map<String, String> result = new HashMap<String, String>();
        EnvInjectJobProperty jobProperty = getEnvInjectJobProperty(build.getParent());
        if (jobProperty != null) {
            result.putAll(getCurrentInjectedEnvVars(build));
        } else {
            result.putAll(getJenkinsSystemVariablesCurrentNode(build));
            result.putAll(getBuildVariables(build));
        }
        return result;
    }

    /**
     * Get a new map with the current envMap
     */
    public Map<String, String> getCurrentInjectedEnvVars(AbstractBuild<?, ?> build) {
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (envInjectAction != null) {
            Map envMap = envInjectAction.getEnvMap();
            result.putAll(envMap);
            return result;
        } else {
            return result;
        }
    }

    public Map<String, String> getCurrentNodeEnvVars() throws IOException, InterruptedException {
        return getEnvVars(Computer.currentComputer());
    }

    private Map<String, String> getEnvVars(Computer computer) throws IOException, InterruptedException {
        Map<String, String> result = new HashMap<String, String>();
        EnvVars envVars = computer.getEnvironment();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
