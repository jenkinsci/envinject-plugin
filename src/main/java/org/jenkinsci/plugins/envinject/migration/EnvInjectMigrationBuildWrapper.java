package org.jenkinsci.plugins.envinject.migration;

import hudson.model.BuildableItemWithBuildWrappers;
import hudson.tasks.BuildWrapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;

/**
 * @author Gregory Boissinot
 */
public abstract class EnvInjectMigrationBuildWrapper extends BuildWrapper {

    /**
     * Gets the new object with the mapped fields
     *
     * @return an EnvInjectBuildWrapper object
     * @param originalItem Original item
     */
    protected abstract EnvInjectBuildWrapper getEnvInjectBuildWrapper(@NonNull BuildableItemWithBuildWrappers originalItem);
}
