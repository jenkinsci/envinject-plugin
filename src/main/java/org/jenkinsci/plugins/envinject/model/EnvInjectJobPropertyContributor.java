package org.jenkinsci.plugins.envinject.model;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.*;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.Serializable;
import java.util.Map;

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
        return Hudson.getInstance().getDescriptor(getClass());
    }

    @SuppressWarnings("unused")
    public static DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(EnvInjectJobPropertyContributor.class);
    }
}
