package hudson.plugins.setenv;

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
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class SetEnvBuildWrapper extends EnvInjectMigrationBuildWrapper {

    private transient String localVarText;

    @Override
    public EnvInjectBuildWrapper getEnvInjectBuildWrapper(@NonNull BuildableItemWithBuildWrappers originalItem) {
        String varText = localVarText;
        EnvInjectBuildWrapper existing = originalItem.getBuildWrappersList().get(EnvInjectBuildWrapper.class);
        if (existing != null && existing.getInfo() != null) {
            String existingContent = existing.getInfo().getPropertiesContent();
            if (existingContent != null && !existingContent.isEmpty()) {
                varText = varText + "\n" + existingContent;
            }
        }
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, varText, null, null, false, null);
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
            return null;
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return false;
        }
    }
}
