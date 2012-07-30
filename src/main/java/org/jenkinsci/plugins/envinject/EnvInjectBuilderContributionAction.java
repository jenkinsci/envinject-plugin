package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilderContributionAction implements EnvironmentContributingAction {

    public static final String ENVINJECT_BUILDER_ACTION_NAME = "EnvInjectBuilderAction";

    private transient Map<String, String> resultVariables;

    public EnvInjectBuilderContributionAction(Map<String, String> resultVariables) {
        this.resultVariables = resultVariables;
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars envVars) {

        if (envVars == null) {
            return;
        }

        if (resultVariables == null) {
            return;
        }

        for (Map.Entry<String, String> entry : resultVariables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null) {
                envVars.put(key, value);
            }
        }
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return ENVINJECT_BUILDER_ACTION_NAME;
    }

    public String getUrlName() {
        return null;
    }

}
