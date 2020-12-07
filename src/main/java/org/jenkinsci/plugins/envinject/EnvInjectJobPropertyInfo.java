package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo implements Describable<EnvInjectJobPropertyInfo> {

    @CheckForNull
    private final String scriptFilePath;
    @CheckForNull
    private final String scriptContent;
    @CheckForNull
    private SecureGroovyScript secureGroovyScript;
    
    /**
     * If enabled, Jenkins will try taking scripts and property files from the master instead of the agent.
     * @since 2.0 Enabled if and only if the global setting allows it.
     * @see EnvInjectPluginConfiguration#enableLoadingFromMaster
     */
    private final boolean loadFilesFromMaster;

    public EnvInjectJobPropertyInfo() {
        this(null, null, null, null, false, null);
    }

    /**
     * Creates the job property definition.
     * @param propertiesFilePath Path to the property file to be injected
     * @param propertiesContent Property definition
     * @param scriptFilePath Path to the Shell/batch script file, which should be executed to retrieve the EnvVars
     * @param scriptContent Shell/batch script, which should be executed to retrieve the EnvVars
     * @param loadFilesFromMaster If {@code true}, the script file will be loaded from the master
     * @param secureGroovyScript Groovy script to be executed in order to produce the environment variables.
     *      This script will be verified by the Script Security plugin if defined.
     */
    @DataBoundConstructor
    public EnvInjectJobPropertyInfo(
            @CheckForNull String propertiesFilePath,
            @CheckForNull String propertiesContent,
            @CheckForNull String scriptFilePath,
            @CheckForNull String scriptContent,
            boolean loadFilesFromMaster,
            @CheckForNull SecureGroovyScript secureGroovyScript
            ) {
        super(propertiesFilePath, propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = fixCrLf(Util.fixEmpty(scriptContent));
        this.secureGroovyScript = secureGroovyScript != null ? secureGroovyScript.configuringWithNonKeyItem() : null;
        this.loadFilesFromMaster = loadFilesFromMaster;
    }


    @Deprecated
    public EnvInjectJobPropertyInfo(
            @CheckForNull String propertiesFilePath,
            @CheckForNull String propertiesContent,
            @CheckForNull String scriptFilePath,
            @CheckForNull String scriptContent,
            @CheckForNull String groovyScriptContent,
            boolean loadFilesFromMaster) {
        
        // If the groovy script is specified, it should become the SecureGroovyScript
        this(propertiesFilePath, propertiesContent, scriptFilePath, scriptContent, loadFilesFromMaster,
                StringUtils.isNotBlank(groovyScriptContent) ? new SecureGroovyScript(groovyScriptContent, false, null).configuring(ApprovalContext.create()) : null);
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
    public SecureGroovyScript getSecureGroovyScript() {
        return secureGroovyScript;
    }

    @CheckForNull
    @Deprecated
    public String getGroovyScriptContent() {
        return secureGroovyScript != null ? Util.fixEmpty(secureGroovyScript.getScript()) : null;
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
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class);
    }

    @CheckForNull
    @Deprecated
    private transient String groovyScriptContent;

    protected Object readResolve() {
        if (secureGroovyScript == null && !StringUtils.isBlank(groovyScriptContent)) {
            secureGroovyScript = new SecureGroovyScript(groovyScriptContent, false, null).configuring(ApprovalContext.create());
            groovyScriptContent = null;
        }
        return this;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EnvInjectJobPropertyInfo> {
        @Override
        public String getDisplayName() {
            return "EnvInjectJobPropertyInfo";
        }
    }
}
