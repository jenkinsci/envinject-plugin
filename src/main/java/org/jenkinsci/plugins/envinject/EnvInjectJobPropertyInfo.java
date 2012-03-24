package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo {

    private String scriptFilePath;
    private String scriptContent;
    private String groovyScriptContent;
    private boolean loadFilesFromMaster;

    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, String groovyScriptContent, boolean loadFilesFromMaster) {
        super(propertiesFilePath, propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = fixCrLf(Util.fixEmpty(scriptContent));
        this.groovyScriptContent = fixCrLf(Util.fixEmpty(groovyScriptContent));
        this.loadFilesFromMaster = loadFilesFromMaster;
    }

    public String getScriptFilePath() {
        return scriptFilePath;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public String getGroovyScriptContent() {
        return groovyScriptContent;
    }

    public boolean isLoadFilesFromMaster() {
        return loadFilesFromMaster;
    }
}
