package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilder extends Builder {

    private EnvInjectInfo info;

    @SuppressWarnings("unused")
    public EnvInjectInfo getInfo() {
        return info;
    }

    public void setInfo(EnvInjectInfo info) {
        this.info = info;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        try {

            //Get env vars from properties
            Map<String, String> envMap = build.getWorkspace().act(new EnvInjectGetEnvVarsFromPropertiesVariables(info, listener));

            //Get a new env vars map with current en vars, build variables and the computed env vars
            //The computed env vars wins
            Map<String, String> buildVariables = build.getEnvironment(listener);
            buildVariables.putAll(build.getBuildVariables());
            buildVariables.putAll(envMap);

            //Resolve vars each other
            EnvVars.resolve(buildVariables);

            //Set the new build variables map
            build.getWorkspace().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(buildVariables)));

            //Add or get the existing action to add new env vars
            addEnvVarsToEnvInjectBuildAction(build, envMap);

        } catch (Throwable throwable) {
            build.setResult(Result.FAILURE);
            return false;
        }

        return true;
    }

    private void addEnvVarsToEnvInjectBuildAction(AbstractBuild<?, ?> build, Map<String, String> envMap) {
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        if (envInjectAction!=null){
            envInjectAction.overrideAll(envMap);
        }
        else {
            build.addAction(new EnvInjectAction(envMap));
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return Messages.envinject_addVars_displayName();
        }

        @Override
        public EnvInjectBuilder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            EnvInjectBuilder envInjectBuilder = new EnvInjectBuilder();
            EnvInjectInfo info = req.bindParameters(EnvInjectInfo.class, "envInjectInfoBuilder.");
            envInjectBuilder.setInfo(info);
            return envInjectBuilder;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help-buildStep.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }
}
