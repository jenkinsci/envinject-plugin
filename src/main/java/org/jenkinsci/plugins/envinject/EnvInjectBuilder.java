package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributor;
import hudson.model.Result;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.LogTaskListener;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.envinject.util.RunHelper;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilder extends Builder implements Serializable {

    @Nonnull 
    private EnvInjectInfo info;

    @DataBoundConstructor
    public EnvInjectBuilder(String propertiesFilePath, String propertiesContent) {
        this.info = new EnvInjectInfo(propertiesFilePath, propertiesContent);
    }

    @Nonnull 
    public EnvInjectInfo getInfo() {
        return info;
    }

    @Override
    public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull Launcher launcher, @Nonnull BuildListener listener) throws InterruptedException, IOException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        logger.info("Injecting environment variables from a build step.");

        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {
            Map<String, String> previousEnvVars = RunHelper.getEnvVarsPreviousSteps(build, logger);

            //Get current envVars
            Map<String, String> variables = new HashMap<String, String>(previousEnvVars);

            //Add workspace if not set
            if (ws != null) {
                if (variables.get("WORKSPACE") == null) {
                    variables.put("WORKSPACE", ws.getRemote());
                }
            }

            //Add SCM variables if not set
            SCM scm = build.getProject().getScm();
            if (scm != null) {
                scm.buildEnvironment(build, variables);
            }

            //Always keep build variables (such as parameter variables).
            variables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            Map<String, String> resultVariables = variables;
            if (ws != null) {
                // File information path can be relative to the workspace.
                // Prop file variables will be merged with other ones
                final Map<String, String> propertiesEnvVars = envInjectEnvVarsService.getEnvVarsFileProperty(ws, logger, info.getPropertiesFilePath(), info.getPropertiesContentMap(previousEnvVars), variables);
                resultVariables  = envInjectEnvVarsService.getMergedVariables(variables, propertiesEnvVars);
            }

            // Whatever we inject, EnvironmentContributors should be able to override that (JENKINS-26583)
            EnvVars contributedEnvVars = new EnvVars();
            RunHelper.consultOtherEnvironmentContributors(build, contributedEnvVars, listener);
            resultVariables.putAll(contributedEnvVars);
                
            build.addAction(new EnvInjectBuilderContributionAction(resultVariables));

            //Add or get the existing action to add new env vars
            envInjectActionSetter.addEnvVarsToRun(build, resultVariables);

        } catch (Throwable throwable) {
            logger.error("Problems occurs on injecting env vars as a build step: " + throwable.getMessage());
            build.setResult(Result.FAILURE);
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getAndAddBuildVariables(@Nonnull AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(build.getBuildVariables());
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            if (result.get("WORKSPACE") == null) {
                result.put("WORKSPACE", ws.getRemote());
            }
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
