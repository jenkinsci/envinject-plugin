package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Descriptor;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.Callable;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectScriptExecutorService;
import org.jenkinsci.plugins.envinject.service.PropertiesVariablesRetriever;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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

        final Map<String, String> resultVariables = new HashMap<String, String>();

        EnvInjectLogger logger = new EnvInjectLogger(listener);

        try {

            final FilePath ws = build.getWorkspace();

            //Add the current system env vars
            ws.act(new Callable<Void, Throwable>() {

                public Void call() throws Throwable {
                    resultVariables.putAll(EnvVars.masterEnvVars);
                    return null;
                }
            });

            //Always keep build variables (such as parameter variables).
            resultVariables.putAll(getAndAddBuildVariables(build));

            //Get env vars from properties info.
            //File information path can be relative to the workspace
            Map<String, String> envMap = ws.act(new PropertiesVariablesRetriever(info, resultVariables, logger));
            resultVariables.putAll(envMap);

            //Execute script info
            EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, resultVariables, ws, launcher, logger);
            scriptExecutorService.executeScriptFromInfoObject();

            // get infos about the triggers and expose it as env variables
			Map<String, String> triggerVariable = getTriggerVariable(info.getAllTriggerVarName(), build);
			resultVariables.putAll(triggerVariable);
			
            //Resolve vars each other
            EnvVars.resolve(resultVariables);

            //Set the new build variables map
            build.getWorkspace().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(resultVariables)));

            //Add or get the existing action to add new env vars
            addEnvVarsToEnvInjectBuildAction(build, resultVariables);

        } catch (Throwable throwable) {
            build.setResult(Result.FAILURE);
        }

        return new EnvironmentImpl();
    }

    class EnvironmentImpl extends Environment {
        @Override
        public void buildEnvVars(Map<String, String> env) {
        }
    }
    
	private Map<String, String> getTriggerVariable(String allTriggerVariableName, AbstractBuild<?, ?> build) {

		StringBuilder all = new StringBuilder();
		Map<String, String> triggerVars = new HashMap<String, String>();
		CauseAction buildCause = build.getAction(CauseAction.class);
		List<Cause> buildCauses = buildCause.getCauses();
		buildCauses.add(new SCMTrigger.SCMTriggerCause("dummy"));
		for (Cause cause : buildCauses) {
			System.out.println("-->" + cause);
			String name = getTriggerName(allTriggerVariableName, cause);
			if (!StringUtils.isBlank(name)) {
				triggerVars.put(name, "true");
				all.append(name);
				all.append(",");
			}

		}

		// setup a variable containing all the trigger names
		if (!StringUtils.isBlank(allTriggerVariableName)) {
			triggerVars.put(allTriggerVariableName, all.toString());
		}
		return triggerVars;
	}

	private static String getTriggerName(String prefix, Cause cause) {
		if (SCMTrigger.SCMTriggerCause.class.isInstance(cause)) {
			return prefix + "_SCMTRIGGER";
		} else if (TimerTrigger.TimerTriggerCause.class.isInstance(cause)) {
			return prefix + "_TIMERTRIGGER";
		} else if (Cause.UserCause.class.isInstance(cause)) {
			return prefix + "_MANUALTRIGGER";
		} else if (Cause.UpstreamCause.class.isInstance(cause)) {
			return prefix + "_UPSTREAMTRIGGER";
		} else if (cause != null) {
			// fallback
			return prefix + "_" + cause.getClass().getSimpleName().toUpperCase();
		}
		return null;
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
