package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectNodeProperty extends NodeProperty<Node> {

    private boolean unsetSystemVariables;

    @DataBoundConstructor
    public EnvInjectNodeProperty(boolean unsetSystemVariables) {
        this.unsetSystemVariables = unsetSystemVariables;
    }

    public boolean isUnsetSystemVariables() {
        return unsetSystemVariables;
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
