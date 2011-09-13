package org.jenkinsci.plugins.envinject;

import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements Action, StaplerProxy {

    public static String URL_NAME = "injectedEnvVarResult";

    private final Map<String, String> envMap;

    public EnvInjectAction(Map<String, String> envMap) {
        this.envMap = envMap;
    }

    public void overrideAll(Map<String, String> all) {
        envMap.putAll(all);
    }

    @SuppressWarnings("unused")
    public Map<String, String> getEnvMap() {
        return envMap;
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
}
