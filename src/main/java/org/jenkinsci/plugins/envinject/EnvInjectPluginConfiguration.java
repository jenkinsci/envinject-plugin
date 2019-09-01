/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.envinject;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.XmlFile;
import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Configuration of security options for {@link EnvInjectPlugin}.
 * @author Oleg Nenashev
 * @since 1.92
 */
@Extension
@Symbol("envInject")
public class EnvInjectPluginConfiguration extends GlobalConfiguration {
    
    private static final EnvInjectPluginConfiguration DEFAULT = 
            new EnvInjectPluginConfiguration(false, false);
            
    private boolean hideInjectedVars;

    private boolean enablePermissions;
    
    /**
     * If enabled, users will be able to use the {@link EnvInjectJobPropertyInfo#loadFilesFromMaster}
     * option.
     * This option is disabled by default due to the potential security concerns (SECURITY-348).
     * @since 2.0
     */
    private boolean enableLoadingFromMaster;

    @DataBoundConstructor
    public EnvInjectPluginConfiguration() {
        load();
    }
    
    /**
     * @deprecated Use {@link #EnvInjectPluginConfiguration(boolean, boolean, boolean)}
     * @since 2.0 Loading of files from master is disabled by default
     */
    @Deprecated
    public EnvInjectPluginConfiguration(boolean hideInjectedVars, boolean enablePermissions) {
        // Breaking change in 2.0
        this(hideInjectedVars, enablePermissions, false);
    }
    
    //TODO: consider enabling it for Groovy Boot Hook Scripts and other configuration-as-code stuff
    /**
     * Constructor.
     * @param hideInjectedVars Hides the Injected Env Vars action in all builds.
     * @param enablePermissions Enables a specific permission for viewing Injected Env Vars
     * @param enableLoadingFromMaster Enables remote loading of property and script files from the master in builds
     */
    /*package*/ EnvInjectPluginConfiguration(boolean hideInjectedVars, boolean enablePermissions, boolean enableLoadingFromMaster) {
        this.hideInjectedVars = hideInjectedVars;
        this.enablePermissions = enablePermissions;
        this.enableLoadingFromMaster = enableLoadingFromMaster;
    }

    public boolean isHideInjectedVars() {
        return hideInjectedVars;
    }

    @DataBoundSetter
    public void setHideInjectedVars(boolean hideInjectedVars) { this.hideInjectedVars = hideInjectedVars; }

    public boolean isEnablePermissions() {
        return enablePermissions;
    }

    @DataBoundSetter
    public void setEnablePermissions(boolean enabledPermissions) { this.enablePermissions = enabledPermissions; }

    /**
     * Check if the instance supports loading of scripts and property files from the master.
     * It does not prevent local loading of files.
     * @return {@code true} if it is enabled
     * @see EnvInjectJobPropertyInfo#loadFilesFromMaster
     * @since 2.0
     */
    public boolean isEnableLoadingFromMaster() {
        return enableLoadingFromMaster;
    }

    @DataBoundSetter
    public void setEnableLoadingFromMaster(boolean enableLoadingFromMaster) { this.enableLoadingFromMaster = enableLoadingFromMaster; }

    /**
     * Gets the default configuration of {@link EnvInjectPlugin}
     * @return Default configuration
     */
    @Nonnull 
    public static final EnvInjectPluginConfiguration getDefault() {
        return DEFAULT;
    }

    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getActiveInstance().getRootDir(), 
                "envinject-plugin-configuration.xml"));
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        final boolean newEnablePermissions = json.getBoolean("enablePermissions");
        final boolean newHideInjectedVars = json.getBoolean("hideInjectedVars");
        final boolean enableLoadingFromMaster = json.getBoolean("enableLoadingFromMaster");
        return configure(newHideInjectedVars, newEnablePermissions, enableLoadingFromMaster);
    }
    
    /**
     * Configuration method for testing purposes.
     * @param hideInjectedVars Hide injected variables actions
     * @param enablePermissions Enables permissions in {@link EnvInjectPlugin}.
     * @return true if the configuration successful
     * @throws IllegalStateException Cannot retrieve the plugin config instance
     */
    @VisibleForTesting
    /*package*/ static boolean configure(boolean hideInjectedVars, boolean enablePermissions, boolean enableLoadingFromMaster)  {
        EnvInjectPluginConfiguration instance = getInstance();
        if (instance == null) {
            throw new IllegalStateException("Cannot retrieve the plugin config instance");
        }
        instance.hideInjectedVars = hideInjectedVars;
        instance.enablePermissions = enablePermissions;
        instance.enableLoadingFromMaster = enableLoadingFromMaster;
        EnvInjectPlugin.getInstance().onConfigChange(instance);
        instance.save();
        return true;
    }
    
    @CheckForNull
    public static EnvInjectPluginConfiguration getInstance() {
        return EnvInjectPluginConfiguration.all().get(EnvInjectPluginConfiguration.class);
    }
    
    /**
     * Retrieves the EnvInject global configuration.
     * @return Settings
     * @throws EnvInjectException The configuration cannot be retrieved
     */
    @Nonnull
    public static EnvInjectPluginConfiguration getOrFail() throws EnvInjectException {
        EnvInjectPluginConfiguration c = EnvInjectPluginConfiguration.all().get(EnvInjectPluginConfiguration.class);
        if (c == null) {
            throw new EnvInjectException("Cannot retrieve the EnvInject plugin configuration");
        }
        return c;
    }
    
    @Override
    public GlobalConfigurationCategory getCategory() {
        return GlobalConfigurationCategory.get(GlobalConfigurationCategory.Security.class);
    }
}
