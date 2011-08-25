package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobPropertyInfo extends EnvInjectInfo {

	private String scriptFilePath;

	private String scriptContent;

	private String allTriggerVarName;

	@DataBoundConstructor
	public EnvInjectJobPropertyInfo(String propertiesFilePath, String propertiesContent, String scriptFilePath, String scriptContent, String allTriggerVarName) {
		super(Util.fixEmpty(propertiesFilePath), Util.fixEmpty(propertiesContent));
		this.scriptFilePath = Util.fixEmpty(scriptFilePath);
		this.scriptContent = Util.fixEmpty(scriptContent);
		this.allTriggerVarName = allTriggerVarName;
	}

	public String getScriptFilePath() {
		return scriptFilePath;
	}

	public String getScriptContent() {
		return scriptContent;
	}

	public String getAllTriggerVarName() {
		return allTriggerVarName;
	}

}
