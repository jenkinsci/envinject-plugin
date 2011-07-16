package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.slaves.NodeProperty;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectWrapper extends BuildWrapper implements Serializable {

    private EnvInjectUIInfo info;

    public EnvInjectUIInfo getInfo() {
        return info;
    }

    public void setInfo(EnvInjectUIInfo info) {
        this.info = info;
    }

    /**
     * Similar to Run.getEnvironment(TaskListener log)} without
     * env = c.getEnvironment().overrideAll(env)
     */
    private Map<String, String> addJenkinsVariable(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        Map<String, String> result = new HashMap<String, String>();

        result.putAll(build.getCharacteristicEnvVars());

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            result.put("JENKINS_URL", rootUrl);
            result.put("HUDSON_URL", rootUrl); // Legacy compatibility
            result.put("BUILD_URL", rootUrl + build.getUrl());
            result.put("JOB_URL", rootUrl + build.getParent().getUrl());
        }
        result.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        result.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility
        Thread t = Thread.currentThread();
        if (t instanceof Executor) {
            Executor e = (Executor) t;
            result.put("EXECUTOR_NUMBER", String.valueOf(e.getNumber()));
            result.put("NODE_NAME", e.getOwner().getName());
            Node n = e.getOwner().getNode();
            if (n != null)
                result.put("NODE_LABELS", Util.join(n.getAssignedLabels(), " "));
        }
        return result;

    }

    @Override
    public Launcher decorateLauncher(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {

        Computer computer = Computer.currentComputer();
        EnvInjectLoadEnv envInjectLoadEnv = new EnvInjectLoadEnv(info, listener);
        final Map<String, String> envMap = new HashMap<String, String>();

        try {
            envMap.putAll(computer.getNode().getRootPath().act(envInjectLoadEnv));

            if (info.isAddJenkinsEnvironmentVariables()) {
                envMap.putAll(addJenkinsVariable(build, listener));
            }

            if (info.isAddNodeEnvironmentVariables()) {

                //Process Global node properties
                for (NodeProperty nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
                    Map<String, String> result = new HashMap<String, String>();
                    hudson.model.Environment environment = nodeProperty.setUp(build, launcher, listener);
                    if (environment != null) {
                        environment.buildEnvVars(result);
                        envMap.putAll(result);
                    }
                }

                //Process Node Properties
                for (NodeProperty nodeProperty : Computer.currentComputer().getNode().getNodeProperties()) {
                    Map<String, String> result = new HashMap<String, String>();
                    hudson.model.Environment environment = nodeProperty.setUp(build, launcher, listener);
                    if (environment != null) {
                        environment.buildEnvVars(result);
                        envMap.putAll(result);
                    }
                }
            }

            //Process Plugins Contribution
            if (info.isAddPluginsEnvironmentVariables()) {
                EnvVars envVars = new EnvVars();
                for (EnvironmentContributor ec : EnvironmentContributor.all()) {
                    ec.buildEnvironmentFor(build, envVars, listener);
                }
                envMap.putAll(envVars);
            }

            //Process Job Parameters
            if (info.isKeepJobParameters()) {

                EnvVars envVars = new EnvVars();
                List<ParametersAction> parametersActions = build.getActions(ParametersAction.class);
                for (ParametersAction parametersAction : parametersActions) {
                    List<ParameterValue> parameterValueList = parametersAction.getParameters();
                    for (ParameterValue parameterValue : parameterValueList) {
                        parameterValue.buildEnvVars(build, envVars);
                    }
                }
                envMap.putAll(envVars);
            }

            EnvVars.resolve(envMap);

        } catch (Throwable throwable) {
            build.setResult(Result.FAILURE);
        }


        try {
            computer.getNode().getRootPath().act(new Callable<Void, Throwable>() {
                public Void call() throws Throwable {
                    Field masterEnvVarsFiled = EnvVars.class.getDeclaredField("masterEnvVars");
                    masterEnvVarsFiled.setAccessible(true);
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(masterEnvVarsFiled, masterEnvVarsFiled.getModifiers() & ~Modifier.FINAL);
                    masterEnvVarsFiled.set(null, envMap);

                    return null;
                }
            });
        } catch (Throwable throwable) {
            build.setResult(Result.FAILURE);
        }

        return launcher;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new EnvironmentImpl();
    }

    class EnvironmentImpl extends Environment {

        @Override
        public void buildEnvVars(Map<String, String> env) {

        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "[Environment Inject] - Inject your environment variables";
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return true;
        }

        @Override
        public EnvInjectWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            EnvInjectWrapper envInjectorWrapper = new EnvInjectWrapper();
            EnvInjectUIInfo info = req.bindParameters(EnvInjectUIInfo.class, "info.");
            envInjectorWrapper.setInfo(info);
            return envInjectorWrapper;
        }
    }

}
