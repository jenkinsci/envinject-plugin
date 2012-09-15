package org.jenkinsci.plugins.envinject;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.service.EnvInjectSavable;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class EnvInjectAction implements Action, StaplerProxy {

    public static String URL_NAME = "injectedEnvVars";

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

    public String getIconFileName() {
        return "document-properties.gif";
    }

    public String getDisplayName() {
        return "Injected Environment Variables";
    }

    public String getUrlName() {
        return URL_NAME;
    }

    public Object getTarget() {
        return new EnvInjectVarList(envMap);
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
            EnvInjectSavable dao = new EnvInjectSavable();
            if (rootDir == null) {
                dao.saveEnvironment(build.getRootDir(), envMap);
                return this;
            }
            dao.saveEnvironment(rootDir, envMap);
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

        EnvInjectSavable dao = new EnvInjectSavable();
        Map<String, String> resultMap = null;
        try {
            if (build != null) {
                resultMap = dao.getEnvironment(build.getRootDir());
            } else if (rootDir != null) {
                resultMap = dao.getEnvironment(rootDir);
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
