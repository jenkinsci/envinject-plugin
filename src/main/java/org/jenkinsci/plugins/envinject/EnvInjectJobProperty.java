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
public class EnvInjectJobProperty extends JobProperty {

    private EnvInjectUIInfo info;

    @SuppressWarnings("unused")
    public EnvInjectUIInfo getInfo() {
        return info;
    }

    public void setInfo(EnvInjectUIInfo info) {
        this.info = info;
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.envinject_displayName();
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
            EnvInjectUIInfo info = req.bindParameters(EnvInjectUIInfo.class, "info.");
            envInjectJobProperty.setInfo(info);
            return envInjectJobProperty;
        }
    }
}
