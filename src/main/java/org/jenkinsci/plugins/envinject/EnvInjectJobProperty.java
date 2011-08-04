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

    private boolean keepSystemVariables;

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

    public void setInfo(EnvInjectJobPropertyInfo info) {
        this.info = info;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setKeepSystemVariables(boolean keepSystemVariables) {
        this.keepSystemVariables = keepSystemVariables;
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
            EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
            EnvInjectJobPropertyInfo info = req.bindParameters(EnvInjectJobPropertyInfo.class, "envInjectInfoJobProperty.");
            envInjectJobProperty.setInfo(info);

            Object onObject = formData.get("on");
            if (onObject != null) {
                envInjectJobProperty.setOn(true);
                if (onObject instanceof JSONObject) {
                    envInjectJobProperty.setKeepSystemVariables(((JSONObject) onObject).getBoolean("keepSystemVariables"));
                }
            } else {
                envInjectJobProperty.setOn(false);
                envInjectJobProperty.setKeepSystemVariables(false);
            }

            return envInjectJobProperty;
        }
    }
}
