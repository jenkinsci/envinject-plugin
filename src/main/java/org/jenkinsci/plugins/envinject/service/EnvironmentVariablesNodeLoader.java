package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.Run;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

// TODO: Restrict?
/**
 * @author Gregory Boissinot
 */
public class EnvironmentVariablesNodeLoader implements Serializable {

    @Deprecated
    public EnvironmentVariablesNodeLoader() {
    }

    /**
     * @deprecated Use {@link #gatherEnvVarsForNode(hudson.model.Run, hudson.model.Node, org.jenkinsci.lib.envinject.EnvInjectLogger)}
     */
    @Nonnull
    @Deprecated
    public Map<String, String> gatherEnvironmentVariablesNode(@Nonnull Run<?, ?> build, 
            @CheckForNull Node buildNode, @Nonnull EnvInjectLogger logger) throws EnvInjectException {
        return gatherEnvVarsForNode(build, buildNode, logger);
    }
    
    @Nonnull
    public static Map<String, String> gatherEnvVarsForNode(@Nonnull Run<?, ?> build, 
            @CheckForNull Node buildNode, @Nonnull EnvInjectLogger logger) throws EnvInjectException {

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
            Map<String, String> nodeEnvVars = nodePath.act(new EnvInjectMasterEnvVarsRetriever());

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
