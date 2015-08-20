package org.jenkinsci.plugins.envinject;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPluginAction extends EnvInjectAction implements EnvironmentContributingAction {

    public EnvInjectPluginAction(AbstractBuild build, Map<String, String> envMap) {
        super(build, envMap);
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
        return new EnvInjectVarList(Maps.transformEntries(envMap,
                new Maps.EntryTransformer<String, String, String>() {
                    public String transformEntry(String key, String value) {
                        final Set<String> sensibleVars = getSensibleVariables();
                        return ((sensibleVars != null) && sensibleVars.contains(key)) ? EnvInjectPlugin.DEFAULT_MASK : value;
                    }
                }));
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        final EnvInjectVarList varList = getEnvInjectVarList();
        Map<String, String> envMap = varList.getEnvMap();
        if (envMap != null) {
            env.putAll(envMap);
        }
    }
}
