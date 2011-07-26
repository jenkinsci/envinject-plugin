package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Main;
import hudson.Platform;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.Callable;
import org.kohsuke.stapler.StaplerProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
