package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilderContributionAction implements EnvironmentContributingAction {

    public static final String ENVINJECT_BUILDER_ACTION_NAME = "EnvInjectBuilderAction";

    @CheckForNull
    private transient Map<String, String> resultVariables;

    public EnvInjectBuilderContributionAction(@CheckForNull Map<String, String> resultVariables) {
        this.resultVariables = resultVariables;
    }

    @Override
    public void buildEnvVars(@Nonnull AbstractBuild<?, ?> build, @CheckForNull EnvVars envVars) {

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

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return ENVINJECT_BUILDER_ACTION_NAME;
    }

    @Override
    public String getUrlName() {
        return null;
    }

}
