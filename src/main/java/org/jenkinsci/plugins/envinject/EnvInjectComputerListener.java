package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.ComputerListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectComputerListener extends ComputerListener implements Serializable {


    private EnvVars getNewMasterEnvironmentVariables(Computer c, FilePath nodePath, TaskListener listener) throws EnvInjectException, IOException, InterruptedException {

        //Get env vars for the current node
        Map<String, String> nodeEnvVars = nodePath.act(
                new Callable<Map<String, String>, IOException>() {
                    public Map<String, String> call() throws IOException {
                        return EnvVars.masterEnvVars;
                    }
                });


        // -- Retrieve Environment variables from master
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        boolean unsetSystemVariables = false;
        Map<String, String> globalPropertiesEnvVars = new HashMap<String, String>();
        for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {

            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                globalPropertiesEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }

            if (nodeProperty instanceof EnvInjectNodeProperty) {
                EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                globalPropertiesEnvVars.putAll(envInjectEnvVarsService.getEnvVarsFileProperty(c.getNode().getRootPath(), logger, envInjectNodeProperty.getPropertiesFilePath(), null, nodeEnvVars));
            }

        }

        //Resolve against node env vars
        envInjectEnvVarsService.resolveVars(globalPropertiesEnvVars, nodeEnvVars);

        EnvVars envVars2Set = new EnvVars();
        if (!unsetSystemVariables) {
            envVars2Set.putAll(nodeEnvVars);
        }
        envVars2Set.putAll(globalPropertiesEnvVars);

        return envVars2Set;
    }

    private EnvVars getNewSlaveEnvironmentVariables(Computer c, FilePath nodePath, TaskListener listener) throws EnvInjectException, IOException, InterruptedException {

        Map<String, String> currentEnvVars = new HashMap<String, String>();

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        //Get env vars for the current node
        Map<String, String> nodeEnvVars = nodePath.act(
                new Callable<Map<String, String>, IOException>() {
                    public Map<String, String> call() throws IOException {
                        return EnvVars.masterEnvVars;
                    }
                });

        // -- Process slave properties
        boolean unsetSystemVariables = false;
        for (NodeProperty<?> nodeProperty : c.getNode().getNodeProperties()) {

            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                currentEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }

            if (nodeProperty instanceof EnvInjectNodeProperty) {
                EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                currentEnvVars.putAll(envInjectEnvVarsService.getEnvVarsFileProperty(c.getNode().getRootPath(), logger, envInjectNodeProperty.getPropertiesFilePath(), null, nodeEnvVars));
            }
        }

        //Resolve against node env vars
        envInjectEnvVarsService.resolveVars(currentEnvVars, nodeEnvVars);

        EnvVars envVars2Set = new EnvVars();
        if (!unsetSystemVariables) {
            envVars2Set.putAll(nodeEnvVars);
        }
        envVars2Set.putAll(currentEnvVars);

        return envVars2Set;

    }


    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {

        //Get node path
        FilePath nodePath = c.getNode().getRootPath();
        if (nodePath == null) {
            return;
        }


        //use case : it is a slave
        if (isActiveSlave(c)) {

            try {
                EnvVars envVars2Set = getNewSlaveEnvironmentVariables(c, nodePath, listener);
                nodePath.act(new EnvInjectMasterEnvVarsSetter(envVars2Set));
            } catch (EnvInjectException e) {
                throw new IOException(e);
            }

        }

        //use case : it is only on master
        else if (isGlobalEnvInjectActivatedOnMaster()) {
            try {
                EnvVars envVars2Set = getNewMasterEnvironmentVariables(c, nodePath, listener);
                nodePath.act(new EnvInjectMasterEnvVarsSetter(envVars2Set));
            } catch (EnvInjectException e) {
                throw new IOException(e);
            }
        }
    }

    private boolean isActiveSlave(Computer c) {
        if (c == null) {
            return false;
        }

        Node slave = Hudson.getInstance().getNode(c.getName());
        return slave != null;
    }

    private boolean isGlobalEnvInjectActivatedOnMaster() {
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Hudson.getInstance().getGlobalNodeProperties();
        for (NodeProperty<?> nodeProperty : globalNodeProperties) {
            if (nodeProperty instanceof EnvInjectNodeProperty) {
                return true;
            }
        }
        return false;

    }

}
