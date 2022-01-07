package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;

import java.io.IOException;
import java.util.Map;

/**
 * @since 1.92
 */
@Extension
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
