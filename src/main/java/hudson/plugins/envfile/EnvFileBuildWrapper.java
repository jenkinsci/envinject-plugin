package hudson.plugins.envfile;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.migration.EnvInjectMigrationBuildWrapper;

import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 */
public class EnvFileBuildWrapper extends EnvInjectMigrationBuildWrapper {

    private transient String filePath;

    @Override
    public EnvInjectBuildWrapper getEnvInjectBuildWrapper(@Nonnull BuildableItemWithBuildWrappers originalItem) {
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(filePath, null, null, null, false, null);
        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper(jobPropertyInfo);
        return envInjectBuildWrapper;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
        };
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return false;
        }
    }
}
