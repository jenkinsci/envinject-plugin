package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return false;
        }

        @Override
        @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "TODO needs triage")
        public String getDisplayName() {
            return null;
        }
    }

}
