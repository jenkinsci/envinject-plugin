package org.jenkinsci.plugins.envinject;

import com.google.common.collect.Maps;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.Map;
import java.util.Set;
import org.jenkinsci.plugins.envinjectapi.util.EnvInjectVarsIO;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 * @deprecated Replaced by {@link EnvInjectPluginAction}.
 */
@Deprecated
public class EnvInjectAction implements Action, StaplerProxy {
    
    /**
     * @deprecated The field does nothing. It was public by mistake
     */
    @Deprecated
    @Restricted(NoExternalUse.class)
    public final static String URL_NAME = "injectedEnvVars";

    private transient Map<String, String> envMap;

    private AbstractBuild build;

    /**
     * Backward compatibility
     */
    private transient Map<String, String> resultVariables;
    private transient File rootDir;

    public EnvInjectAction(AbstractBuild build, Map<String, String> envMap) {
        this.build = build;
        this.envMap = envMap;
    }

    public void overrideAll(Map<String, String> all) {
        envMap.putAll(all);
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Map<String, String> getEnvMap() {
        return UnmodifiableMap.decorate(envMap);
    }

    @Override
    public String getIconFileName() {
        if (!EnvInjectPlugin.canViewInjectedVars(build)) {
            return null;
        }
        return "document-properties.gif";
    }

    @Override
    public String getDisplayName() {
        return "Environment Variables";
    }

    @Override
    public String getUrlName() {
        if (!EnvInjectPlugin.canViewInjectedVars(build)) {
            return null;
        }
        return "injectedEnvVars";
    }

    protected AbstractBuild getBuild() {
        return build;
    }
    
    @Override
    public Object getTarget() {
        final Set sensitiveVariables = build.getSensitiveBuildVariables();
        if (!EnvInjectPlugin.canViewInjectedVars(build)) {
            return EnvInjectVarList.HIDDEN;
        }
        
        return new EnvInjectVarList(Maps.transformEntries(envMap,
                new Maps.EntryTransformer<String, String, String>() {
                    public String transformEntry(String key, String value) {
                        return sensitiveVariables.contains(key) ? EnvInjectPlugin.DEFAULT_MASK : value;
                    }
                }));
    }

    @SuppressWarnings("unused")
    private Object writeReplace() throws ObjectStreamException {

        if (envMap == null) {
            return this;
        }

        if (envMap.size() == 0) {
            return this;
        }

        if (build == null) {
            return this;
        }

        try {
            if (rootDir == null) {
                EnvInjectVarsIO.saveEnvironment(build.getRootDir(), envMap);
                return this;
            }
            EnvInjectVarsIO.saveEnvironment(rootDir, envMap);
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }
        return this;
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {

        if (resultVariables != null) {
            envMap = resultVariables;
            return this;
        }

        Map<String, String> resultMap = null;
        try {
            if (build != null) {
                resultMap = EnvInjectVarsIO.getEnvironment(build.getRootDir());
            } else if (rootDir != null) {
                resultMap = EnvInjectVarsIO.getEnvironment(rootDir);
            }
            if (resultMap != null) {
                envMap = resultMap;
            }
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }

        return this;
    }
}
