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
    public void buildEnvironmentFor(Job job, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        EnvInjectJobProperty jobProperty = (EnvInjectJobProperty) job.getProperty(EnvInjectJobProperty.class);
        if (jobProperty != null && jobProperty.isOn()) {
            EnvInjectJobPropertyInfo jobPropertyInfo = jobProperty.getInfo();

            // Processes "Properties Content"
            Map<String, String> result = jobPropertyInfo.getPropertiesContentMap(env);
            if (result != null) {
                env.putAll(result);
            }
        }
    }
}
