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
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import static org.jenkinsci.plugins.envinject.EnvInjectPlugin.getJenkinsInstance;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Configuration of {@link EnvInjectPlugin}.
 * @author Oleg Nenashev
 * @since 1.92
 */
@Extension
public class EnvInjectPluginConfiguration extends GlobalConfiguration {
    
    private static final EnvInjectPluginConfiguration DEFAULT = 
            new EnvInjectPluginConfiguration(false, false);
            
    private boolean hideInjectedVars;

    private boolean enablePermissions;

    public EnvInjectPluginConfiguration() {
        load();
    }
    
    public EnvInjectPluginConfiguration(boolean hideInjectedVars, boolean enablePermissions) {
        this.hideInjectedVars = hideInjectedVars;
        this.enablePermissions = enablePermissions;
    }

    public boolean isHideInjectedVars() {
        return hideInjectedVars;
    }

    public boolean isEnablePermissions() {
        return enablePermissions;
    }
 
    /**
     * Gets the default configuration of {@link EnvInjectPlugin}
     * @return Default configuration
     */
    public static final @Nonnull EnvInjectPluginConfiguration getDefault() {
        return DEFAULT;
    }

    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(Jenkins.XSTREAM, new File(getJenkinsInstance().getRootDir(), 
                "envinject-plugin-configuration.xml"));
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        final boolean newEnablePermissions = json.getBoolean("enablePermissions");
        final boolean newHideInjectedVars = json.getBoolean("hideInjectedVars");
        return configure(newHideInjectedVars, newEnablePermissions);
    }
    
    /**
     * Configuration method for testing purposes.
     * @param hideInjectedVars Hide injected variables actions
     * @param enablePermissions Enables permissions in {@link EnvInjectPlugin}.
     * @return true if the configuration successful
     * @throws IllegalStateException Cannot retrieve the plugin config instance
     */
    @VisibleForTesting
    /*package*/ static boolean configure(boolean hideInjectedVars, boolean enablePermissions)  {
        EnvInjectPluginConfiguration instance = getInstance();
        if (instance == null) {
            throw new IllegalStateException("Cannot retrieve the plugin config instance");
        }
        instance.hideInjectedVars = hideInjectedVars;
        instance.enablePermissions = enablePermissions;
        EnvInjectPlugin.getInstance().onConfigChange(instance);
        instance.save();
        return true;
    }
    
    @CheckForNull
    public static EnvInjectPluginConfiguration getInstance() {
        return EnvInjectPluginConfiguration.all().get(EnvInjectPluginConfiguration.class);
    }
}
