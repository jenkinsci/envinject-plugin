package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectNodeProperty extends NodeProperty<Node> {

    private boolean unsetSystemVariables;

    private String propertiesFilePath;

    @DataBoundConstructor
    public EnvInjectNodeProperty(boolean unsetSystemVariables, String propertiesFilePath) {
        this.unsetSystemVariables = unsetSystemVariables;
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
    }

    public boolean isUnsetSystemVariables() {
        return unsetSystemVariables;
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    @Extension
    public static class EnvInjectNodePropertyDescriptor extends NodePropertyDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.envinject_nodeProperty_displayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help-node.html";
        }
    }
}
