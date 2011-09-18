package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.envinject.service.*;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuildWrapper extends BuildWrapper implements Serializable {

    private EnvInjectJobPropertyInfo info;

    public void setInfo(EnvInjectJobPropertyInfo info) {
        this.info = info;
    }

    @SuppressWarnings("unused")
    public EnvInjectJobPropertyInfo getInfo() {
        return info;
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);

        //Get current envVars
        Map<String, String> variables = envInjectActionSetter.getCurrentEnvVars(build);

        try {

            //Always keep build variables (such as parameter variables).
            variables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            //File information path can be relative to the workspace
            Map<String, String> envMap = ws.act(new PropertiesVariablesRetriever(info, variables, logger));
            variables.putAll(envMap);

            //Execute script info
            EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, variables, ws, launcher, logger);
            scriptExecutorService.executeScriptFromInfoObject();

            // Retrieve triggered cause
            if (info.isPopulateTriggerCause()) {
                Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(build);
                variables.putAll(triggerVariable);
            }

            //Resolve vars each other
            EnvVars.resolve(variables);

            //Remove unset variables
            final Map<String, String> resultVariables = new EnvInjectEnvVarsUnset(logger).removeUnsetVars(variables);

            //Add or get the existing action to add new env vars
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build, resultVariables);

            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.putAll(resultVariables);
                }
            };

        } catch (Throwable throwable) {
            logger.error("[EnvInject] - [ERROR] - Problems occurs on injecting env vars as a build wrap: " + throwable.getMessage());
            build.setResult(Result.FAILURE);
            return null;
        }
    }

    private Map<String, String> getAndAddBuildVariables(AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();
        //Add build variables such as parameters
        result.putAll(build.getBuildVariables());
        //Add workspace variable
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            result.put("WORKSPACE", ws.getRemote());
        }
        return result;
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.envinject_wrapper_displayName();
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
            EnvInjectJobPropertyInfo info = req.bindParameters(EnvInjectJobPropertyInfo.class, "envInjectInfoWrapper.");
            wrapper.setInfo(info);
            return wrapper;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help-buildWrapper.html";
        }
    }
}
