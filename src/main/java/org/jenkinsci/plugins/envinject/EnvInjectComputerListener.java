package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsRetriever;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectComputerListener extends ComputerListener implements Serializable {

    private NavigableMap<String, String> getNewMasterEnvironmentVariables(@NonNull Computer c,
            @NonNull FilePath nodePath, @NonNull TaskListener listener) throws EnvInjectException, IOException, InterruptedException {

        //Get env vars for the current node
        Map<String, String> nodeEnvVars = nodePath.act(new EnvInjectMasterEnvVarsRetriever());

        // -- Retrieve Environment variables from master
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        boolean unsetSystemVariables = false;
        Map<String, String> globalPropertiesEnvVars = new HashMap<>();
        for (NodeProperty<?> nodeProperty : Jenkins.get().getGlobalNodeProperties()) {

            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                globalPropertiesEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }

            final Node node = c.getNode();
            if (node != null && nodeProperty instanceof EnvInjectNodeProperty) {
                EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                final FilePath rootPath = node.getRootPath();
                if (rootPath == null) {
                    throw new EnvInjectException("Node is offline, cannot calculate the injected variables");
                }
                globalPropertiesEnvVars.putAll(envInjectEnvVarsService.getEnvVarsFileProperty(
                        rootPath, logger, envInjectNodeProperty.getPropertiesFilePath(), 
                        null, nodeEnvVars));
            }

        }

        //Resolve against node env vars
        envInjectEnvVarsService.resolveVars(globalPropertiesEnvVars, nodeEnvVars);

        NavigableMap<String, String> envVars2Set = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!unsetSystemVariables) {
            envVars2Set.putAll(nodeEnvVars);
        }
        envVars2Set.putAll(globalPropertiesEnvVars);

        return envVars2Set;
    }

    private NavigableMap<String, String> getNewSlaveEnvironmentVariables(@NonNull Computer c,
            @NonNull FilePath nodePath, @NonNull TaskListener listener) throws EnvInjectException, IOException, InterruptedException {

        Map<String, String> currentEnvVars = new HashMap<>();

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        final Node node = c.getNode();
        if (node == null) {
            throw new EnvInjectException("Node is removed, but the computer has not gone yet");
        } 
        
        //Get env vars for the current node
        Map<String, String> nodeEnvVars = nodePath.act(new EnvInjectMasterEnvVarsRetriever());

        // -- Process slave properties
        boolean unsetSystemVariables = false;
        for (NodeProperty<?> nodeProperty : node.getNodeProperties()) {

            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                currentEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }

            if (nodeProperty instanceof EnvInjectNodeProperty) {
                EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                final FilePath rootPath = node.getRootPath();
                if (rootPath == null) {
                    throw new EnvInjectException("Node is offline, cannot calculate the injected variables");
                }
                currentEnvVars.putAll(envInjectEnvVarsService.getEnvVarsFileProperty(rootPath, logger, envInjectNodeProperty.getPropertiesFilePath(), null, nodeEnvVars));
            }
        }

        //Resolve against node env vars
        envInjectEnvVarsService.resolveVars(currentEnvVars, nodeEnvVars);

        NavigableMap<String, String> envVars2Set = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!unsetSystemVariables) {
            envVars2Set.putAll(nodeEnvVars);
        }
        envVars2Set.putAll(currentEnvVars);

        return envVars2Set;

    }


    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {

        //Get node path
        final Node node = c.getNode();
        final FilePath nodePath = node != null ? node.getRootPath() : null;
        if (nodePath == null) {
            return;
        }


        //use case : it is a slave
        if (isActiveSlave(c)) {

            try {
                NavigableMap<String, String> envVars2Set = getNewSlaveEnvironmentVariables(c, nodePath, listener);
                nodePath.act(new EnvInjectMasterEnvVarsSetter(envVars2Set));
            } catch (EnvInjectException e) {
                throw new IOException(e);
            }

        }

        //use case : it is only on master
        else if (isGlobalEnvInjectActivatedOnMaster()) {
            try {
                NavigableMap<String, String> envVars2Set = getNewMasterEnvironmentVariables(c, nodePath, listener);
                nodePath.act(new EnvInjectMasterEnvVarsSetter(envVars2Set));
            } catch (EnvInjectException e) {
                throw new IOException(e);
            }
        }
    }

    private boolean isActiveSlave(@CheckForNull Computer c) {
        if (c == null) {
            return false;
        }

        Node slave = Jenkins.get().getNode(c.getName());
        return slave != null;
    }

    private boolean isGlobalEnvInjectActivatedOnMaster() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Jenkins.get().getGlobalNodeProperties();
        for (NodeProperty<?> nodeProperty : globalNodeProperties) {
            if (nodeProperty instanceof EnvInjectNodeProperty) {
                return true;
            }
        }
        return false;

    }
}
