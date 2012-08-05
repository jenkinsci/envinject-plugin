package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.OutputStream;
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
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return super.decorateLogger(build, logger);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        logger.info("Executing scripts and injecting environment variables after the SCM step.");

        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {

            Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);
            Map<String, String> injectedEnvVars = new HashMap<String, String>(previousEnvVars);

            //Add workspace if not set
            if (ws != null) {
                injectedEnvVars.put("WORKSPACE", ws.getRemote());
            }

            //Add SCM variables if not set
            SCM scm = build.getProject().getScm();
            if (scm != null) {
                scm.buildEnvVars(build, injectedEnvVars);
            }

            //Get result variables
            Map<String, String> propertiesEnvVars = envInjectEnvVarsService.getEnvVarsPropertiesProperty(ws, logger, info.getPropertiesFilePath(), info.getPropertiesContentMap(previousEnvVars), injectedEnvVars);

            //Resolve variables
            final Map<String, String> resultVariables = envInjectEnvVarsService.getMergedVariables(injectedEnvVars, propertiesEnvVars);

            //Execute script info
            int resultCode = envInjectEnvVarsService.executeScript(info.getScriptContent(), ws, info.getScriptFilePath(), resultVariables, launcher, listener);
            if (resultCode != 0) {
                logger.info("Fail the build.");
                build.setResult(Result.FAILURE);
                return null;
            }

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
