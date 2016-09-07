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
    private final boolean loadFilesFromMaster;

    public EnvInjectJobPropertyInfo() {
        this(null, null, null, null, false, null);
    }

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
        super(propertiesFilePath, propertiesContent);
        this.scriptFilePath = Util.fixEmpty(scriptFilePath);
        this.scriptContent = fixCrLf(Util.fixEmpty(scriptContent));
        secureGroovyScript = new SecureGroovyScript(groovyScriptContent, false, null).configuring(ApprovalContext.create());
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
    public SecureGroovyScript getSecureGroovyScript() {
        return secureGroovyScript;
    }

    @CheckForNull
    @Deprecated
    public String getGroovyScriptContent() {
        return secureGroovyScript != null ? Util.fixEmpty(secureGroovyScript.getScript()) : null;
    }

    public boolean isLoadFilesFromMaster() {
        return loadFilesFromMaster;
    }

    @Override
    public Descriptor<EnvInjectJobPropertyInfo> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @CheckForNull
    @Deprecated
    private transient String groovyScriptContent;

    @SuppressWarnings("deprecation")
    public Object readResolve() {
        if (!StringUtils.isBlank(groovyScriptContent)) {
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
