package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectInfo implements Serializable {

    protected String propertiesFilePath;

    protected String propertiesContent;

    protected boolean populateTriggerCause;

    @DataBoundConstructor
    public EnvInjectInfo(String propertiesFilePath, String propertiesContent, boolean populateTriggerCause) {
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
        this.propertiesContent = Util.fixEmpty(propertiesContent);
        this.populateTriggerCause = populateTriggerCause;
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public String getPropertiesContent() {
        return propertiesContent;
    }

    public boolean isPopulateTriggerCause() {
        return populateTriggerCause;
    }
}
