package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilder extends Builder implements Serializable {

    private EnvInjectInfo info;

    @DataBoundConstructor
    public EnvInjectBuilder(String propertiesFilePath, String propertiesContent) {
        this.info = new EnvInjectInfo(propertiesFilePath, propertiesContent, false);
    }

    @SuppressWarnings("unused")
    public EnvInjectInfo getInfo() {
        return info;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {

            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            Map<String, String> previousEnvVars = variableGetter.getPreviousEnvVars(build, logger);

            //Get current envVars
            Map<String, String> variables = new HashMap<String, String>(previousEnvVars);

            //Always keep build variables (such as parameter variables).
            variables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            //File information path can be relative to the workspace
            final Map<String, String> resultVariables = envInjectEnvVarsService.getEnvVarsPropertiesProperty(ws, logger, info.getPropertiesFilePath(), info.getPropertiesContent(), variables);

            //Set the new build variables map
            build.addAction(new EnvironmentContributingAction() {
                public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
                    env.putAll(resultVariables);
                }

                public String getIconFileName() {
                    return null;
                }

                public String getDisplayName() {
                    return null;
                }

                public String getUrlName() {
                    return null;
                }
            });

            //Add or get the existing action to add new env vars
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build, resultVariables);

        } catch (Throwable throwable) {
            logger.error("[EnvInject] - [ERROR] - Problems occurs on injecting env vars as a build step: " + throwable.getMessage());
            build.setResult(Result.FAILURE);
            return false;
        }

        return true;
    }

    private Map<String, String> getAndAddBuildVariables(AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(build.getBuildVariables());
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            result.put("WORKSPACE", ws.getRemote());
        }
        return result;
    }


    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return Messages.envinject_addVars_displayName();
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
