package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;

import java.io.IOException;
import java.util.Map;

/**
 * Contributes environment values to the environment.
 *
 * This extension has the low ordinal, and hence
 * it will be processed first in {@link EnvironmentContributor#all()} reversed iterators.
 * {@link jenkins.model.CoreEnvironmentContributor} has {@code -100}.
 * Reverse ones are used in {@link Job#getEnvironment(Node, TaskListener)}...
 * It means that other contributors will be always able to override values contributed by EnvInject
 *
 * @since 1.92
 */
@Extension(ordinal = -99)
public class EnvInjectEnvVarsContributor extends EnvironmentContributor {

    @Override
    @SuppressWarnings("rawtypes")
    public void buildEnvironmentFor(Job job, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        EnvInjectJobProperty jobProperty = (EnvInjectJobProperty) job.getProperty(EnvInjectJobProperty.class);
        if (jobProperty != null && jobProperty.isOn()) {
            EnvInjectJobPropertyInfo jobPropertyInfo = jobProperty.getInfo();

            // Processes "Properties Content"
            if (jobPropertyInfo != null) {
                Map<String, String> result = jobPropertyInfo.getPropertiesContentMap(env);
                if (result != null) {
                    int expectedEnvSize = env.size() + result.size();
                    env.putAll(result);
                    if (env.size() != expectedEnvSize) {
                        listener.error("Not all environment variables could be successfully injected. " +
                                "Check for similarly-named environment variables.");
                    }
                }
            }
        }
    }
}
