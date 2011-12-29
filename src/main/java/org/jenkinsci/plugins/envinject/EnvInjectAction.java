package org.jenkinsci.plugins.envinject;

import hudson.model.Action;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.plugins.envinject.service.EnvInjectSaveable;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements Action, StaplerProxy {

    public static String URL_NAME = "injectedEnvVars";

    private File rootDir;

    private transient Map<String, String> envMap;

    public EnvInjectAction(File rootDir, Map<String, String> envMap) {
        this.rootDir = rootDir;
        this.envMap = envMap;
    }

    public void overrideAll(Map<String, String> all) {
        envMap.putAll(all);
    }

    @SuppressWarnings("unused")
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
        try {
            EnvInjectSaveable dao = new EnvInjectSaveable();
            dao.saveEnvironment(rootDir, envMap);
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }
        return this;
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        EnvInjectSaveable dao = new EnvInjectSaveable();
        Map<String, String> resultMap = null;
        try {
            //Backward compatibility: the result is null, the map is maybe in the action itself; therefore no action
            if (rootDir != null) {
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
