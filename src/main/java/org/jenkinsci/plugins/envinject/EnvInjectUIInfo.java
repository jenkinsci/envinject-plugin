package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectUIInfo implements Serializable {

    private boolean on;

    private String propertiesFilePath;

    private String propertiesContent;

    private String scriptFilePath;

    private String scriptContent;

    private boolean keepSystemVariables;

    @DataBoundConstructor
    public EnvInjectUIInfo(boolean on, String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, boolean keepSystemVariables) {
        this.on = on;
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
        this.propertiesContent = Util.fixEmpty(propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = Util.fixEmpty(scriptContent);
        this.keepSystemVariables = keepSystemVariables;
    }

    public boolean isOn() {
        return on;
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public String getPropertiesContent() {
        return propertiesContent;
    }

    public String getScriptFilePath() {
        return scriptFilePath;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public boolean isKeepSystemVariables() {
        return keepSystemVariables;
    }
}
