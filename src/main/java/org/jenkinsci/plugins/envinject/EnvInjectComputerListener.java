package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectComputerListener extends ComputerListener implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectComputerListener.class.getName());

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {

        //Default value to false (even if no checked)
        boolean unsetSystemVariables = false;

        //Global Properties
        for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvInjectNodeProperty) {
                EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
            }
        }

        Node slave = Hudson.getInstance().getNode(c.getName());
        //Specific nodeProperties can overrides the value if this is a slave
        if (slave != null) {
            for (NodeProperty<?> nodeProperty : c.getNode().getNodeProperties()) {
                if (nodeProperty instanceof EnvInjectNodeProperty) {
                    EnvInjectNodeProperty envInjectNodeProperty = ((EnvInjectNodeProperty) nodeProperty);
                    unsetSystemVariables = envInjectNodeProperty.isUnsetSystemVariables();
                }
            }
        }

        //Remove System vars is necessary
        if (unsetSystemVariables) {
            try {
                c.getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (EnvInjectException e) {
                e.printStackTrace();
            }
        }
    }
}
