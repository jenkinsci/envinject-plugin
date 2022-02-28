package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import java.util.Arrays;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectNodeProperty extends NodeProperty<Node> {

    private boolean unsetSystemVariables;

    @CheckForNull
    private final String propertiesFilePath;

    @DataBoundConstructor
    public EnvInjectNodeProperty(boolean unsetSystemVariables, String propertiesFilePath) {
        this.unsetSystemVariables = unsetSystemVariables;
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
    }

    public boolean isUnsetSystemVariables() {
        return unsetSystemVariables;
    }

    @CheckForNull
    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    @Extension
    public static class EnvInjectNodePropertyDescriptor extends NodePropertyDescriptor {

        @NonNull
        private EnvInjectGlobalPasswordEntry[] envInjectGlobalPasswordEntries = new EnvInjectGlobalPasswordEntry[0];
        public static final String ENVINJECT_CONFIG = "envInject";

        public EnvInjectNodePropertyDescriptor() {
            load();
        }

        public EnvInjectNodePropertyDescriptor(Class<? extends NodeProperty<?>> clazz) {
            super(clazz);
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.envinject_nodeProperty_displayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help-node.html";
        }

        public EnvInjectGlobalPasswordEntry[] getEnvInjectGlobalPasswordEntries() {
            return Arrays.copyOf(envInjectGlobalPasswordEntries, envInjectGlobalPasswordEntries.length);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            List<EnvInjectGlobalPasswordEntry> envInjectGlobalPasswordEntriesList = req.bindParametersToList(EnvInjectGlobalPasswordEntry.class, "envInject.");
            envInjectGlobalPasswordEntries = envInjectGlobalPasswordEntriesList.toArray(new EnvInjectGlobalPasswordEntry[envInjectGlobalPasswordEntriesList.size()]);
            save();
            return true;
        }

        @Override
        public String getId() {
            return ENVINJECT_CONFIG;
        }

    }
}
