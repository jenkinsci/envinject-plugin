package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo implements Describable<EnvInjectJobPropertyInfo> {

    @CheckForNull
    private final String scriptFilePath;
    @CheckForNull
    private final String scriptContent;
    @CheckForNull
    private final String groovyScriptContent;
    
    /**
     * If enabled, .
     * @since 2.0 Enabled if and only if the global setting allows it.
     * @see EnvInjectPluginConfiguration#enableLoadingFromMaster
     */
    private final boolean loadFilesFromMaster;

    public EnvInjectJobPropertyInfo() {
        this(null, null, null, null, null, false);
    }

    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(
            @CheckForNull String propertiesFilePath, 
            @CheckForNull String propertiesContent, 
            @CheckForNull String scriptFilePath, 
            @CheckForNull String scriptContent, 
            @CheckForNull String groovyScriptContent, 
            boolean loadFilesFromMaster) {
        super(propertiesFilePath, propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = fixCrLf(Util.fixEmpty(scriptContent));
        this.groovyScriptContent = fixCrLf(Util.fixEmpty(groovyScriptContent));
        this.loadFilesFromMaster = loadFilesFromMaster;
    }

    @CheckForNull
    public String getScriptFilePath() {
        return scriptFilePath;
    }

    @CheckForNull
    public String getScriptContent() {
        return scriptContent;
    }

    @CheckForNull
    public String getGroovyScriptContent() {
        return groovyScriptContent;
    }

    /**
     * Check if the configuration requires loading of script and property files from the master.
     * @return {@code true} if the loading from the master is required.
     *         Note that this option may be rejected due to the value of {@link EnvInjectPluginConfiguration#enableLoadingFromMaster}.
     */
    public boolean isLoadFilesFromMaster() {
        return loadFilesFromMaster;
    }

    @Override
    public Descriptor<EnvInjectJobPropertyInfo> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EnvInjectJobPropertyInfo> {
        @Override
        public String getDisplayName() {
            return "EnvInjectJobPropertyInfo";
        }
    }
}
