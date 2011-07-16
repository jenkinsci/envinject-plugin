package org.jenkinsci.plugins.envinject;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectUIInfo implements Serializable {

    private String propertiesFilePath;

    private String propertiesContent;

    private boolean addNodeEnvironmentVariables;

    private boolean addJenkinsEnvironmentVariables;

    private boolean keepJobParameters;

    private boolean addPluginsEnvironmentVariables;

    @DataBoundConstructor
    public EnvInjectUIInfo(String propertiesFilePath, String propertiesContent, boolean addNodeEnvironmentVariables, boolean addJenkinsEnvironmentVariables, boolean keepJobParameters, boolean addPluginsEnvironmentVariables) {
        this.propertiesFilePath = propertiesFilePath;
        this.propertiesContent = propertiesContent;
        this.addNodeEnvironmentVariables = addNodeEnvironmentVariables;
        this.addJenkinsEnvironmentVariables = addJenkinsEnvironmentVariables;
        this.keepJobParameters = keepJobParameters;
        this.addPluginsEnvironmentVariables = addPluginsEnvironmentVariables;
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public String getPropertiesContent() {
        return propertiesContent;
    }

    public boolean isAddNodeEnvironmentVariables() {
        return addNodeEnvironmentVariables;
    }

    public boolean isAddJenkinsEnvironmentVariables() {
        return addJenkinsEnvironmentVariables;
    }

    public boolean isKeepJobParameters() {
        return keepJobParameters;
    }

    public boolean isAddPluginsEnvironmentVariables() {
        return addPluginsEnvironmentVariables;
    }
}
