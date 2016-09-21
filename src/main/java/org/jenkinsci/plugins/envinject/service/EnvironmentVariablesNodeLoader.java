package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
public class EnvironmentVariablesNodeLoader implements Serializable {

    public Map<String, String> gatherEnvironmentVariablesNode(AbstractBuild build, Node buildNode, EnvInjectLogger logger) throws EnvInjectException {

        logger.info("Loading node environment variables.");

        if (buildNode == null) {
            return Collections.emptyMap();
        }

        FilePath nodePath = buildNode.getRootPath();
        if (nodePath == null) {
            return Collections.emptyMap();
        }

        //Default node envVars
        Map<String, String> configNodeEnvVars = new HashMap<String, String>();

        try {

            //Get env vars for the current node
            Map<String, String> nodeEnvVars = nodePath.act(
                    new MasterToSlaveCallable<Map<String, String>, IOException>() {
                        public Map<String, String> call() throws IOException {
                            return EnvVars.masterEnvVars;
                        }
                    }
            );

            for (NodeProperty<?> nodeProperty : Jenkins.getActiveInstance().getGlobalNodeProperties()) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    EnvironmentVariablesNodeProperty variablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                    EnvVars envVars = variablesNodeProperty.getEnvVars();
                    EnvInjectEnvVars envInjectEnvVars = new EnvInjectEnvVars(logger);
                    configNodeEnvVars.putAll(envVars);
                    envInjectEnvVars.resolveVars(configNodeEnvVars, nodeEnvVars);
                }
            }

            for (NodeProperty<?> nodeProperty : buildNode.getNodeProperties()) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    EnvironmentVariablesNodeProperty variablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                    EnvVars envVars = variablesNodeProperty.getEnvVars();
                    EnvInjectEnvVars envInjectEnvVars = new EnvInjectEnvVars(logger);
                    configNodeEnvVars.putAll(envVars);
                    envInjectEnvVars.resolveVars(configNodeEnvVars, nodeEnvVars);
                }
            }

            return configNodeEnvVars;

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }
    }

}
