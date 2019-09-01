/*
 * The MIT License (MIT)
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

import hudson.Plugin;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Stores permissions for EnvInject plugin.
 * @author Oleg Nenashev
 * @since 1.92
 */
@ExportedBean
public class EnvInjectPlugin extends Plugin {

    private final static Logger LOGGER = Logger.getLogger(EnvInjectPlugin.class.getName());

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(EnvInjectPlugin.class, Messages._envinject_permissions_title());

    public static final String DEFAULT_MASK = "[*******]";

    /**
     * Allows to view injected variables.
     * Even Jenkins admins may have no such permission in particular installations.
     */
    public static final Permission VIEW_INJECTED_VARS = new Permission(PERMISSIONS, "ViewVars", Messages._envinject_permissions_viewVars_description(), null, PermissionScope.RUN);
     
    /**
     * Retrieves the plugin instance.
     * @return {@link EnvInjectPlugin}
     * @throws IllegalStateException the plugin has not been loaded yet
     */
    public static @Nonnull EnvInjectPlugin getInstance() {
        EnvInjectPlugin plugin = Jenkins.getActiveInstance().getPlugin(EnvInjectPlugin.class);
        if (plugin == null) { // Fail horribly
            // TODO: throw a graceful error
            throw new IllegalStateException("Cannot get the plugin's instance. Jenkins or the plugin have not been initialized yet");
        }
        return plugin;
    }
    
    /*package*/ void onConfigChange(@Nonnull EnvInjectPluginConfiguration config) {
       VIEW_INJECTED_VARS.setEnabled(config.isEnablePermissions()); 
    }

    @Nonnull
    public EnvInjectPluginConfiguration getConfiguration() {
        final EnvInjectPluginConfiguration config = EnvInjectPluginConfiguration.getInstance();
        return config != null ? config : EnvInjectPluginConfiguration.getDefault();
    }
    
    /**
     * Checks if the current user can view injected variables in the run.
     * @param run Run to be checked
     * @return true if the injected variables can be displayed.
     */
    @Restricted(NoExternalUse.class)
    public static boolean canViewInjectedVars(@Nonnull Run<?,?> run) {
        // We allow security engines to block the output
        if (VIEW_INJECTED_VARS.getEnabled() &&  !run.hasPermission(VIEW_INJECTED_VARS)) {
            return false;
        }
        
        // Last check - global configs
        final EnvInjectPluginConfiguration configuration = getInstance().getConfiguration();
        return !configuration.isHideInjectedVars();
    }
    
    @Override 
    public void start() throws Exception {
        VIEW_INJECTED_VARS.setEnabled(getConfiguration().isEnablePermissions());
    }
}
