package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class EnvInjectGlobalPasswordWrapper extends BuildWrapper {


    @DataBoundConstructor
    public EnvInjectGlobalPasswordWrapper() {
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
        };
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }

}
