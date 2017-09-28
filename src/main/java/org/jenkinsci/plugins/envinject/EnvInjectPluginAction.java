package org.jenkinsci.plugins.envinject;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import java.io.IOException;
import java.util.Collections;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.RunAction2;
import org.jenkinsci.plugins.envinject.util.RunHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction implements EnvironmentContributingAction {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectPluginAction.class.getName());

    /**
     * Constructor.
     * @deprecated This is a {@link RunAction2} instance, not need to pass build explicitly.
     *             Use {@link #EnvInjectPluginAction(java.util.Map)}
     */
    @Deprecated
    public EnvInjectPluginAction(@Nonnull AbstractBuild build, @CheckForNull Map<String, String> envMap) {
        super(build, envMap);
    }
    
    /**
     * Constructor.
     * @param envMap Environment variables to be injected
     * @since 2.1
     */
    public EnvInjectPluginAction(@CheckForNull Map<String, String> envMap) {
        super(envMap);
    }

    @Override
    public String getIconFileName() {
        if (!EnvInjectPlugin.canViewInjectedVars(getOwner())) {
            return null;
        }
        return super.getIconFileName();
    }

    @Override
    public String getUrlName() {
        if (!EnvInjectPlugin.canViewInjectedVars(getOwner())) {
            return null;
        }
        return super.getUrlName();
    }
 
    @Override
    public Object getTarget() {
        if (!EnvInjectPlugin.canViewInjectedVars(getOwner())) {
            return EnvInjectVarList.HIDDEN;
        }
        return getEnvInjectVarList();
    }
    
    @Nonnull
    private EnvInjectVarList getEnvInjectVarList() {
        final Map<String, String> currentEnvMap = getEnvMap();
        if (currentEnvMap == null) {
            return new EnvInjectVarList(Collections.<String,String>emptyMap());
        }
        return new EnvInjectVarList(Maps.transformEntries(currentEnvMap,
                new Maps.EntryTransformer<String, String, String>() {
                    public String transformEntry(String key, String value) {
                        final Set<String> sensibleVars = getSensibleVariables();
                        return ((sensibleVars != null) && sensibleVars.contains(key)) ? EnvInjectPlugin.DEFAULT_MASK : value;
                    }
                }));
    }

    // The method is synchronized, because it modifies the internal cache
    @Override
    public synchronized void buildEnvVars(@Nonnull AbstractBuild<?, ?> build, @Nonnull EnvVars env) {
        final Map<String, String> currentEnvMap = getEnvMap();
        if (currentEnvMap == null) {
            return; // Nothing to inject
        }

        // Other extension points may contribute other variable values
        // before contributing actions is invoked. See AbstractBuild#getEnvironment()
        // We take the externally updated variables as a source of truth and just override the missing ones.
        // Otherwise it causes JENKINS-26583
        Map<String, String> overrides = null;
        for (Map.Entry<String, String> storedVar : currentEnvMap.entrySet()) {
            final String varName = storedVar.getKey();
            final String storedValue = storedVar.getValue();
            final String envValue = env.get(storedVar.getKey());
            if (envValue == null) {
                LOGGER.log(Level.CONFIG, "Build {0}: Variable {1} is missing, overriding it by the stored value {2}",
                        new Object[] {build, varName, storedValue});
                env.put(varName, storedValue);
            } else if (!envValue.equals(storedValue)) {
                LOGGER.log(Level.CONFIG, "Build {0}: Variable {1} is defined externally, overriding the stored value {2} by {3}",
                        new Object[] {build, varName, storedValue, envValue});
                if (overrides == null) {
                    overrides = new HashMap<>();
                }
                overrides.put(varName, envValue);
            }
        }

        if (overrides != null) {
            LOGGER.log(Level.FINER, "Build {0}: Overriding {1} variables, which have been changed since the previous run",
                    new Object[] {build, overrides.size()});
            overrideAll(RunHelper.getSensitiveBuildVariables(build), overrides);
            // TODO: We do not save the action at this point,
            // it should be persisted by the AbstractBuild later when the build completes
            // Should we?
            // try {
            //     getOwner().save();
            // } catch (IOException ex) {
            //    LOGGER.log(Level.WARNING, "Failed to persist EnvInject variable overrides", ex);
            // }
        }
    }
}
