package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo {

    private String scriptFilePath;

    private String scriptContent;

    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, boolean populateTriggerCause) {
        super(Util.fixEmpty(propertiesFilePath), Util.fixEmpty(propertiesContent), populateTriggerCause);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = Util.fixEmpty(scriptContent);
    }

    public String getScriptFilePath() {
        return scriptFilePath;
    }

    public String getScriptContent() {
        return scriptContent;
    }
}
