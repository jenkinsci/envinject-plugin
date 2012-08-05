package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
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
        this.info = new EnvInjectInfo(propertiesFilePath, propertiesContent);
    }

    @SuppressWarnings("unused")
    public EnvInjectInfo getInfo() {
        return info;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        logger.info("Injecting environment variables from a build step.");

        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {

            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);

            //Get current envVars
            Map<String, String> variables = new HashMap<String, String>(previousEnvVars);

            //Add workspace if not set
            if (ws != null) {
                variables.put("WORKSPACE", ws.getRemote());
            }

            //Add SCM variables if not set
            SCM scm = build.getProject().getScm();
            if (scm != null) {
                scm.buildEnvVars(build, variables);
            }

            //Always keep build variables (such as parameter variables).
            variables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            //File information path can be relative to the workspace
            final Map<String, String> propertiesEnvVars = envInjectEnvVarsService.getEnvVarsPropertiesProperty(ws, logger, info.getPropertiesFilePath(), info.getPropertiesContentMap(previousEnvVars), variables);

            //Resolve variables
            final Map<String, String> resultVariables = envInjectEnvVarsService.getMergedVariables(variables, propertiesEnvVars);

            //Set the new build variables map
            build.addAction(new EnvInjectBuilderContributionAction(resultVariables));

            //Add or get the existing action to add new env vars
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build, resultVariables);

        } catch (Throwable throwable) {
            logger.error("[EnvInject] - [ERROR] - Problems occurs on injecting env vars as a build step: " + throwable.getMessage());
            build.setResult(Result.FAILURE);
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
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
