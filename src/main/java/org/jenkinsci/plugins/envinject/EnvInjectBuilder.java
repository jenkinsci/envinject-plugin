package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.envinject.service.PropertiesVariablesRetriever;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuilder extends Builder implements Serializable {

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

        final Map<String, String> resultVariables = new HashMap<String, String>();

        try {

            FilePath ws = build.getWorkspace();

            //Fix JENKINS-10847 postponed
/*
            //Add the current system env vars
            resultVariables.putAll(ws.act(new Callable<Map<String, String>, Throwable>() {
                public Map<String, String> call() throws Throwable {
                    return EnvVars.masterEnvVars;
                }
            }));
*/
            //Always keep build variables (such as parameter variables).
            resultVariables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            //File information path can be relative to the workspace
            Map<String, String> envMap = build.getWorkspace().act(new PropertiesVariablesRetriever(info, resultVariables, new EnvInjectLogger(listener)));
            resultVariables.putAll(envMap);

            //Resolve vars each other
            EnvVars.resolve(resultVariables);

            //Fix JENKINS-10847 postponed
            //Set the new build variables map
            //ws.act(new EnvInjectMasterEnvVarsSetter(new EnvVars(resultVariables)));
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
            addEnvVarsToEnvInjectBuildAction(build, resultVariables);

        } catch (Throwable throwable) {
            build.setResult(Result.FAILURE);
            return false;
        }

        return true;
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

    private void addEnvVarsToEnvInjectBuildAction(AbstractBuild<?, ?> build, Map<String, String> envMap) {
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        if (envInjectAction != null) {
            envInjectAction.overrideAll(envMap);
        } else {
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
