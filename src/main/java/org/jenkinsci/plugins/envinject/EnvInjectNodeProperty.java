package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        private static final Logger LOGGER = Logger.getLogger(EnvInjectNodePropertyDescriptor.class.getName());

        private EnvInjectGlobalPasswordEntry[] envInjectGlobalPasswordEntries = new EnvInjectGlobalPasswordEntry[0];
        private static final String ENVINJECT_CONFIG = "envInject.xml";

        @SuppressWarnings("unused")
        public EnvInjectNodePropertyDescriptor() {
            load();
        }

        @SuppressWarnings("unused")
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

        @SuppressWarnings("unused")
        public EnvInjectGlobalPasswordEntry[] getEnvInjectGlobalPasswordEntries() {
            return envInjectGlobalPasswordEntries;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            List<EnvInjectGlobalPasswordEntry> envInjectGlobalPasswordEntriesList = req.bindParametersToList(EnvInjectGlobalPasswordEntry.class, "envInject.");
            envInjectGlobalPasswordEntries = envInjectGlobalPasswordEntriesList.toArray(new EnvInjectGlobalPasswordEntry[envInjectGlobalPasswordEntriesList.size()]);
            save();
            return true;
        }

        @Override
        public void save() {
            try {
                getConfigFile().write(this);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save global passwords ", e);
            }
        }

        @Override
        public synchronized void load() {
            XmlFile file = getConfigFile();
            if (!file.exists())
                return;

            try {
                file.unmarshal(this);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load " + file, e);
            }
        }

        public static XmlFile getConfigFile() {
            return new XmlFile(new File(Hudson.getInstance().getRootDir(), ENVINJECT_CONFIG));
        }
    }
}