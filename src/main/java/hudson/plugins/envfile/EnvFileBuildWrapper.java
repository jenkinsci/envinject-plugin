package hudson.plugins.envfile;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.migration.EnvInjectMigrationBuildWrapper;

/**
 * @author Gregory Boissinot
 */
public class EnvFileBuildWrapper extends EnvInjectMigrationBuildWrapper {

	private transient String filePath;

	@Override
	public EnvInjectBuildWrapper getEnvInjectBuildWrapper() {
		EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(filePath, null, null, null, false);
		EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper();
		envInjectBuildWrapper.setInfo(jobPropertyInfo);
		return envInjectBuildWrapper;
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
