package org.jenkinsci.plugins.envinject;

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

        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {

            Map<String, String> nodeEnvVars = getNodeEnvVars(envInjectEnvVarsService);
            Map<String, String> currentEnvInjectEnvVars = getCurrentEnvInjectEnvVars(envInjectActionSetter, build);
            Map<String, String> buildEnvVars = getBuildVariables(build);
            Map<String, String> envVarsForFilePath = getEnvVarsForFilePath(envInjectEnvVarsService, nodeEnvVars, currentEnvInjectEnvVars, buildEnvVars);
            Map<String, String> propertiesEnvVars = retrievePropertiesVars(ws, logger, envVarsForFilePath);

            Map<String, String> previousEnvVars = new HashMap<String, String>();
            previousEnvVars.putAll(nodeEnvVars);
            previousEnvVars.putAll(currentEnvInjectEnvVars);

            Map<String, String> injectedEnvVars = new HashMap<String, String>();
            injectedEnvVars.putAll(previousEnvVars);
            injectedEnvVars.putAll(buildEnvVars);
            injectedEnvVars.putAll(propertiesEnvVars);

            //Execute script info
            EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, envVarsForFilePath, injectedEnvVars, ws, launcher, logger);
            scriptExecutorService.executeScriptFromInfoObject();

            // Retrieve triggered cause
            if (info.isPopulateTriggerCause()) {
                Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(build);
                injectedEnvVars.putAll(triggerVariable);
            }

            //Resolves vars each other
            envInjectEnvVarsService.resolveVars(injectedEnvVars, previousEnvVars);

            //Remove unset variables
            final Map<String, String> resultVariables = envInjectEnvVarsService.removeUnsetVars(injectedEnvVars);

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

    private Map<String, String> getNodeEnvVars(EnvInjectEnvVars envInjectEnvVarsService) {
        return envInjectEnvVarsService.getCurrentNodeEnvVars();
    }

    private Map<String, String> getCurrentEnvInjectEnvVars(EnvInjectActionSetter envInjectActionSetter, AbstractBuild build) {
        return envInjectActionSetter.getCurrentEnvVars(build);
    }

    private Map<String, String> getEnvVarsForFilePath(EnvInjectEnvVars envInjectEnvVarsService, Map<String, String> nodeVars, Map<String, String> currentEnvInjectEnvVars, Map<String, String> buildVars) {
        Map<String, String> buildVarsForFilePath = new HashMap<String, String>();
        buildVarsForFilePath.putAll(nodeVars);
        buildVarsForFilePath.putAll(currentEnvInjectEnvVars);
        buildVarsForFilePath.putAll(buildVars);
        return buildVarsForFilePath;
    }

    private Map<String, String> retrievePropertiesVars(FilePath ws, EnvInjectLogger logger, Map<String, String> buildVarsForFilePath) throws IOException, InterruptedException {
        Map<String, String> envMap = ws.act(new PropertiesVariablesRetriever(info, buildVarsForFilePath, logger));
        return envMap;
    }

    private Map<String, String> getBuildVariables(AbstractBuild build) {
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
