package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo implements Describable<EnvInjectJobPropertyInfo> {

    private String scriptFilePath;
    private String scriptContent;
    private String groovyScriptContent;
    private boolean loadFilesFromMaster;

    public EnvInjectJobPropertyInfo() {
        super(null, null);
    }

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

    @Override
    public Descriptor<EnvInjectJobPropertyInfo> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EnvInjectJobPropertyInfo> {
        @Override
        public String getDisplayName() {
            return "EnvInjectJobPropertyInfo";
        }
    }
}
