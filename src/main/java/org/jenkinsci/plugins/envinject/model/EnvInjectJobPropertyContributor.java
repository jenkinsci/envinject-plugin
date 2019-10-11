package org.jenkinsci.plugins.envinject.model;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.*;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
public abstract class EnvInjectJobPropertyContributor implements ExtensionPoint, Describable<EnvInjectJobPropertyContributor>, Serializable {

    /**
     * Call it in order to fill in fields of the type
     */
    public abstract void init();

    /**
     * @deprecated Use {@link #contributeEnvVarsToRun(hudson.model.Run, hudson.model.TaskListener, java.util.Map)}
     */
    @Deprecated
    public Map<String, String> getEnvVars(
            @Nonnull AbstractBuild build, 
            @Nonnull TaskListener listener) throws EnvInjectException {
        return Collections.emptyMap();
    }

    /**
     * Contributes environment variables to the run.
     * @param run Run
     * @param listener Log listener
     * @param envVars Target collection
     * @throws EnvInjectException Environment injection error
     * @since 2.1
     */
    @Nonnull
    public void contributeEnvVarsToRun(
            @Nonnull Run<?, ?> run, 
            @Nonnull TaskListener listener,
            @Nonnull Map<String, String> envVars) throws EnvInjectException {
        
        // Fallback to the old method for AbstractBuilds, do nothing for other types
        if (run instanceof AbstractBuild) {
            Map<String, String> res = getEnvVars((AbstractBuild)run, listener);
            envVars.putAll(res);
        }
    }
    
    public Descriptor<EnvInjectJobPropertyContributor> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    public static DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor> all() {
        return Jenkins.get().getDescriptorList(EnvInjectJobPropertyContributor.class);
    }
}
