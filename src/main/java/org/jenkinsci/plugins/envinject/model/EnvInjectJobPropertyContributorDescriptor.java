package org.jenkinsci.plugins.envinject.model;

import hudson.model.Descriptor;

/**
 * @author Gregory Boissinot
 */
public abstract class EnvInjectJobPropertyContributorDescriptor extends Descriptor<EnvInjectJobPropertyContributor> {

    @SuppressWarnings("unused")
    protected EnvInjectJobPropertyContributorDescriptor(Class<? extends EnvInjectJobPropertyContributor> clazz) {
        super(clazz);
    }

    @SuppressWarnings("unused")
    protected EnvInjectJobPropertyContributorDescriptor() {
    }
}
