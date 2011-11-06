package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo {

    private String scriptFilePath;

    private String scriptContent;

    private boolean loadFilesFromMaster;

    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, boolean populateTriggerCause, boolean loadFilesFromMaster) {
        super(propertiesFilePath, propertiesContent, populateTriggerCause);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = Util.fixEmpty(scriptContent);
        this.loadFilesFromMaster = loadFilesFromMaster;
    }

    public String getScriptFilePath() {
        return scriptFilePath;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public boolean isLoadFilesFromMaster() {
        return loadFilesFromMaster;
    }
}
