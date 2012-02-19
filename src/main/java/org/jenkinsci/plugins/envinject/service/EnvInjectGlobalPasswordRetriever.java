package org.jenkinsci.plugins.envinject.service;

import hudson.XmlFile;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectGlobalPasswordEntry;
import org.jenkinsci.plugins.envinject.EnvInjectNodeProperty;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectGlobalPasswordRetriever implements Serializable {

    public EnvInjectGlobalPasswordEntry[] getGlobalPasswords() throws EnvInjectException {
        XmlFile xmlFile = EnvInjectNodeProperty.EnvInjectNodePropertyDescriptor.getConfigFile();
        if (xmlFile.exists()) {
            EnvInjectNodeProperty.EnvInjectNodePropertyDescriptor desc;
            try {
                desc = (EnvInjectNodeProperty.EnvInjectNodePropertyDescriptor) xmlFile.read();
            } catch (IOException ioe) {
                throw new EnvInjectException(ioe);
            }
            return desc.getEnvInjectGlobalPasswordEntries();
        }

        return null;
    }
}