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

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        try {
            EnvInjectLogger logger = new EnvInjectLogger(listener);
            EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

            //Get node path
            FilePath nodePath = c.getNode().getRootPath();
            if (nodePath == null) {
                return;
            }

            //Default value to false (even if no checked)
            boolean unsetSystemVariables = false;

            //Default properties vars
            Map<String, String> globalPropertiesEnvVars = new HashMap<String, String>();

            //Get env vars for the current node
            Map<String, String> nodeEnvVars = nodePath.act(
                    new Callable<Map<String, String>, IOException>() {
                        public Map<String, String> call() throws IOException {
                            return EnvVars.masterEnvVars;
                        }
                    }
            );

            //Global Properties
            for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {

                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    globalPropertiesEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                }

                if (nodeProperty instanceof EnvInjectNodeProperty) {

                    Map<String, String> masterEnvVars = new HashMap<String, String>();
                    try {
                        masterEnvVars = Hudson.getInstance().getRootPath().act(
                                new Callable<Map<String, String>, Throwable>() {
                                    public Map<String, String> call() throws Throwable {
                                        return EnvVars.masterEnvVars;
                                    }
                                }
                        );
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                    EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                    unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();

                    //Add global properties
                    globalPropertiesEnvVars.putAll(envInjectEnvVarsService.getEnvVarsPropertiesProperty(c.getNode().getRootPath(), logger, envInjectNodeProperty.getPropertiesFilePath(), null, masterEnvVars));
                }
            }


            Node slave = Hudson.getInstance().getNode(c.getName());

            //Specific nodeProperties can overrides the value if this is a slave
            if (slave != null) {
                for (NodeProperty<?> nodeProperty : c.getNode().getNodeProperties()) {

                    if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        globalPropertiesEnvVars.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                    }

                    if (nodeProperty instanceof EnvInjectNodeProperty) {
                        EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                        unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                        globalPropertiesEnvVars.putAll(envInjectEnvVarsService.getEnvVarsPropertiesProperty(c.getNode().getRootPath(), logger, envInjectNodeProperty.getPropertiesFilePath(), null, nodeEnvVars));
                    }
                }
            }

            //Resolve against node env vars
            envInjectEnvVarsService.resolveVars(globalPropertiesEnvVars, nodeEnvVars);

            EnvVars envVars2Set = new EnvVars();
            if (!unsetSystemVariables) {
                envVars2Set.putAll(nodeEnvVars);
            }
            envVars2Set.putAll(globalPropertiesEnvVars);

            //Set new env vars
            nodePath.act(new EnvInjectMasterEnvVarsSetter(envVars2Set));

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }
    }
}
