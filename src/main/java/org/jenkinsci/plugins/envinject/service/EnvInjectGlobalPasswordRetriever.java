package org.jenkinsci.plugins.envinject.service;

import hudson.XmlFile;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectGlobalPasswordEntry;
import org.jenkinsci.plugins.envinject.EnvInjectNodeProperty;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectGlobalPasswordRetriever implements Serializable {

    @CheckForNull 
    public EnvInjectGlobalPasswordEntry[] getGlobalPasswords() throws EnvInjectException {
        XmlFile xmlFile = new XmlFile(new File(Jenkins.get().getRootDir(), EnvInjectNodeProperty.EnvInjectNodePropertyDescriptor.ENVINJECT_CONFIG + ".xml"));
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
