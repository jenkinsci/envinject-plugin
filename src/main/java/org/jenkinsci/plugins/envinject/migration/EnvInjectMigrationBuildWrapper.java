package org.jenkinsci.plugins.envinject.migration;

import hudson.tasks.BuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;

/**
 * @author Gregory Boissinot
 */
public abstract class EnvInjectMigrationBuildWrapper extends BuildWrapper {

    /**
     * Gets the new object with the mapped fields
     *
     * @return an EnvInjectBuildWrapper object
     */
    protected abstract EnvInjectBuildWrapper getEnvInjectBuildWrapper();
}
