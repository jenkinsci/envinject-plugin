package org.jenkinsci.plugins.envinject;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import java.util.Collections;

import hudson.model.ParametersAction;
import hudson.model.Run;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import jenkins.model.RunAction2;
import org.jenkinsci.plugins.envinject.util.RunHelper;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction implements EnvironmentContributingAction {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectPluginAction.class.getName());

    /**
     * Cache of resolved parameters, which is stored within this action.
     * This cache assumes that the parameters never change after the creation of the action.
     * It is technically possible via API, but there is no realistic use-case for that.
     * Famous last words(c)
     */
    @GuardedBy("this")
    private transient EnvVars resolvedParameterEnvVars = null;

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

    @CheckForNull
    private synchronized EnvVars getParameterEnvVars() {
        final Run<?, ?> run = getOwner();
        if (resolvedParameterEnvVars == null && run instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)run;
            EnvVars resolvedParameters = new EnvVars();

            List<ParametersAction> actions = build.getActions(ParametersAction.class);
            for (ParametersAction params : actions) {
                params.buildEnvironment(build, resolvedParameters);
            }
            resolvedParameterEnvVars = resolvedParameters;
        }
        return resolvedParameterEnvVars;
    }

    // The method is synchronized, because it modifies the internal cache
    @Override
    public synchronized void buildEnvVars(@Nonnull AbstractBuild<?, ?> build, @Nonnull EnvVars env) {
        assert build == getOwner() : "Trying to resolve environment for build, which is not an owner of this action";

        final Map<String, String> currentEnvMap = getEnvMap();
        if (currentEnvMap == null) {
            return; // Nothing to inject
        }

        // Other extension points may contribute other variable values
        // before contributing actions is invoked. See AbstractBuild#getEnvironment()
        // We take the externally updated variables as a source of truth and just override the missing ones
        Map<String, String> overrides = null;
        for (Map.Entry<String, String> storedVar : currentEnvMap.entrySet()) {
            final String varName = storedVar.getKey();
            final String storedValue = storedVar.getValue();
            final String envValue = env.get(varName);
            if (envValue == null) {
                LOGGER.log(Level.CONFIG, "Build {0}: Variable {1} is missing, overriding it by value stored in the action",
                        new Object[] {build, varName});
                env.put(varName, storedValue);
            } else if (!envValue.equals(storedValue)) {
                // If the value is defined by the Parameters, we actually override them
                // See org.jenkinsci.plugins.envinject.EnvInjectJobPropertyTest#shouldOverrideBuildParametersIfEnabled()
                final EnvVars parameterEnvVars = getParameterEnvVars();
                boolean usedExternalValue = true;
                if (parameterEnvVars != null) {
                    String parameterValue = parameterEnvVars.get(varName);
                    if (envValue.equals(parameterValue)) { // defined by parameter and not already overridden
                        final EnvInjectJobProperty prop = RunHelper.getEnvInjectJobProperty(build);
                        if (prop != null && prop.isOverrideBuildParameters()) {
                            LOGGER.log(Level.CONFIG, "Build {0}: Overriding value of {1} defined by the parameter value",
                                    new Object[] {build, varName});
                            env.put(varName, storedValue);
                            usedExternalValue = false;
                        } else {
                            LOGGER.log(Level.CONFIG, "Build {0}: Build variable {1} will not be overridden, overriding value stored in the action",
                                    new Object[] {build, varName});
                            if (overrides == null) {
                                overrides = new HashMap<>();
                            }
                            overrides.put(varName, envValue);
                        }
                    }
                }

                if (usedExternalValue) { // The value was overridden, let's update the cache
                    LOGGER.log(Level.CONFIG, "Build {0}: Variable {1} is defined externally, overriding value stored in the action",
                            new Object[] {build, varName});
                    if (overrides == null) {
                        overrides = new HashMap<>();
                    }
                    overrides.put(varName, envValue);
                }
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
