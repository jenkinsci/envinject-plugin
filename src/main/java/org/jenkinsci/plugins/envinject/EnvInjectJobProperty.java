package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectJobProperty<T extends Job<?, ?>> extends JobProperty<T> {

    private EnvInjectJobPropertyInfo info;
    private boolean on;
    private boolean keepJenkinsSystemVariables;
    private boolean keepBuildVariables;

    private transient boolean keepSystemVariables;

    @SuppressWarnings("unused")
    public EnvInjectJobPropertyInfo getInfo() {
        return info;
    }

    @SuppressWarnings("unused")
    public boolean isOn() {
        return on;
    }

    @SuppressWarnings("unused")
    public boolean isKeepSystemVariables() {
        return keepSystemVariables;
    }

    @SuppressWarnings("unused")
    public boolean isKeepJenkinsSystemVariables() {
        return keepJenkinsSystemVariables;
    }

    @SuppressWarnings("unused")
    public boolean isKeepBuildVariables() {
        return keepBuildVariables;
    }

    public void setInfo(EnvInjectJobPropertyInfo info) {
        this.info = info;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setKeepJenkinsSystemVariables(boolean keepJenkinsSystemVariables) {
        this.keepJenkinsSystemVariables = keepJenkinsSystemVariables;
    }

    public void setKeepBuildVariables(boolean keepBuildVariables) {
        this.keepBuildVariables = keepBuildVariables;
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "[Environment Inject] -" + Messages.envinject_set_displayName();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help.html";
        }

        @Override
        public EnvInjectJobProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            Object onObject = formData.get("on");

            if (onObject != null) {
                EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
                EnvInjectJobPropertyInfo info = req.bindParameters(EnvInjectJobPropertyInfo.class, "envInjectInfoJobProperty.");
                envInjectJobProperty.setInfo(info);
                envInjectJobProperty.setOn(true);
                if (onObject instanceof JSONObject) {
                    envInjectJobProperty.setKeepJenkinsSystemVariables(((JSONObject) onObject).getBoolean("keepJenkinsSystemVariables"));
                    envInjectJobProperty.setKeepBuildVariables(((JSONObject) onObject).getBoolean("keepBuildVariables"));
                    return envInjectJobProperty;
                }
            }

            return null;
        }
    }

}
