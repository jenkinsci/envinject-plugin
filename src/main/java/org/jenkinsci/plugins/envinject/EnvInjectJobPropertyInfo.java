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
    private String groovyScriptContentEnvVariables;
    private String groovyScriptFiles;

    private boolean loadFilesFromMaster;

    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath,
    		String scriptContent, String groovyScriptContent, String groovyScriptContentEnvVariables, String groovyScriptFiles,
    		boolean loadFilesFromMaster) {
        super(propertiesFilePath, propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = fixCrLf(Util.fixEmpty(scriptContent));
        this.groovyScriptContent = fixCrLf(Util.fixEmpty(groovyScriptContent));
        this.loadFilesFromMaster = loadFilesFromMaster;
        this.groovyScriptFiles = groovyScriptFiles;
        this.groovyScriptContentEnvVariables = groovyScriptContentEnvVariables;
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
    public String getGroovyScriptContentEnvVariables() {
        return groovyScriptContentEnvVariables;
    }
    
    public String getGroovyScriptFiles() {
        return groovyScriptFiles;
    }

    public boolean isLoadFilesFromMaster() {
        return loadFilesFromMaster;
    }
}
