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
        if (envVars != null) {
            envVars.putAll(resultVariables);
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
