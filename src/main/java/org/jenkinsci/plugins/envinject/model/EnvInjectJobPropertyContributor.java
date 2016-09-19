package org.jenkinsci.plugins.envinject.model;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.*;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.Serializable;
import java.util.Map;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
public abstract class EnvInjectJobPropertyContributor implements ExtensionPoint, Describable<EnvInjectJobPropertyContributor>, Serializable {

    /**
     * Call it in order to fill in fields of the type
     */
    public abstract void init();

    public abstract Map<String, String> getEnvVars(AbstractBuild build, TaskListener listener) throws EnvInjectException;

    public Descriptor<EnvInjectJobPropertyContributor> getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptor(getClass());
    }

    @SuppressWarnings("unused")
    public static DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor> all() {
        return Jenkins.getActiveInstance().getDescriptorList(EnvInjectJobPropertyContributor.class);
    }
}
