package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo {

	private String scriptFilePath;

	private String scriptContent;

	private boolean populateCauseEnv;

	@DataBoundConstructor
	public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, boolean populateCauseEnv) {
		super(Util.fixEmpty(propertiesFilePath), Util.fixEmpty(propertiesContent));
		this.scriptFilePath = Util.fixEmpty(scriptFilePath);
		this.scriptContent = Util.fixEmpty(scriptContent);
		this.populateCauseEnv = populateCauseEnv;
	}

	public String getScriptFilePath() {
		return scriptFilePath;
	}

	public String getScriptContent() {
		return scriptContent;
	}

	public boolean isPopulateCauseEnv() {
		return populateCauseEnv;
	}

}
