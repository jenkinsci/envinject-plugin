package hudson.plugins.setenv;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.migration.EnvInjectMigrationBuildWrapper;

import java.io.IOException;

/**
 * @author Gregory Boissinot
 */
public class SetEnvBuildWrapper extends EnvInjectMigrationBuildWrapper {

    @SuppressWarnings("unused")
    private transient String localVarText;

    @Override
    public EnvInjectBuildWrapper getEnvInjectBuildWrapper() {
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, localVarText, null, null, null, false);
        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper();
        envInjectBuildWrapper.setInfo(jobPropertyInfo);
        return envInjectBuildWrapper;
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
        public String getDisplayName() {
            return null;
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return false;
        }
    }
}
