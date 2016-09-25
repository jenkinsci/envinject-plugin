package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import hudson.Util;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.Hudson.MasterComputer;
import hudson.util.LogTaskListener;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectVariableGetter {

    private static Logger LOG = Logger.getLogger(EnvInjectVariableGetter.class.getName());

    @Nonnull
    public Map<String, String> getJenkinsSystemVariables(boolean forceOnMaster) throws IOException, InterruptedException {

        Map<String, String> result = new TreeMap<String, String>();

        final Computer computer;
        final Jenkins jenkins = Jenkins.getActiveInstance();
        if (forceOnMaster) {
            
            computer = jenkins.toComputer();
        } else {
            computer = Computer.currentComputer();
        }

        //test if there is at least one executor
        if (computer != null) {
            result = computer.getEnvironment().overrideAll(result);
            if(computer instanceof MasterComputer) {
                result.put("NODE_NAME", "master");
            } else {
                result.put("NODE_NAME", computer.getName());
            }
            
            Node n = computer.getNode();
            if (n != null) {
                result.put("NODE_LABELS", Util.join(n.getAssignedLabels(), " "));
            }
        }

        String rootUrl = jenkins.getRootUrl();
        if (rootUrl != null) {
            result.put("JENKINS_URL", rootUrl);
            result.put("HUDSON_URL", rootUrl); // Legacy compatibility
        }
        result.put("JENKINS_HOME", jenkins.getRootDir().getPath());
        result.put("HUDSON_HOME", jenkins.getRootDir().getPath());   // legacy compatibility

        return result;
    }


    @SuppressWarnings("unchecked")
    public Map<String, String> getBuildVariables(@Nonnull AbstractBuild build, @Nonnull EnvInjectLogger logger) throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        //Add build process variables
        result.putAll(build.getCharacteristicEnvVars());

        try {
            EnvVars envVars = new EnvVars();
            for (EnvironmentContributor ec : EnvironmentContributor.all()) {
                ec.buildEnvironmentFor(build, envVars, new LogTaskListener(LOG, Level.ALL));
                result.putAll(envVars);
            }

            JDK jdk = build.getProject().getJDK();
            if (jdk != null) {
                Node node = build.getBuiltOn();
                if (node != null) {
                    jdk = jdk.forNode(node, logger.getListener());
                }
                jdk.buildEnvVars(result);
            }
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }

        Executor e = build.getExecutor();
        if (e != null) {
            result.put("EXECUTOR_NUMBER", String.valueOf(e.getNumber()));
        }

        String rootUrl = Jenkins.getActiveInstance().getRootUrl();
        if (rootUrl != null) {
            result.put("BUILD_URL", rootUrl + build.getUrl());
            result.put("JOB_URL", rootUrl + build.getParent().getUrl());
        }

        //Add build variables such as parameters, plugins contributions, ...
        result.putAll(build.getBuildVariables());

        //Retrieve triggered cause
        Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(build);
        result.putAll(triggerVariable);

        return result;
    }

    @CheckForNull
    @SuppressWarnings("unchecked")
    public EnvInjectJobProperty getEnvInjectJobProperty(@Nonnull AbstractBuild build) {
        if (build == null) {
            throw new IllegalArgumentException("A build object must be set.");
        }

        final Job job;
        if (build instanceof MatrixRun) {
            job = ((MatrixRun) build).getParentBuild().getParent();
        } else {
            job = build.getParent();
        }

        EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) job.getProperty(EnvInjectJobProperty.class);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                return envInjectJobProperty;
            }
        }
        return null;
    }

    @Nonnull
    public Map<String, String> getEnvVarsPreviousSteps(
            @Nonnull AbstractBuild build, @Nonnull EnvInjectLogger logger) 
            throws IOException, InterruptedException, EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        List<Environment> environmentList = build.getEnvironments();
        if (environmentList != null) {
            for (Environment e : environmentList) {
                if (e != null) {
                    e.buildEnvVars(result);
                }
            }
        }

        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            result.putAll(getCurrentInjectedEnvVars(envInjectAction));
            //Add build variables with axis for a MatrixRun
            if (build instanceof MatrixRun) {
                result.putAll(build.getBuildVariables());
            }
        } else {
            result.putAll(getJenkinsSystemVariables(false));
            result.putAll(getBuildVariables(build, logger));
        }
        return result;
    }

    @Nonnull
    private Map<String, String> getCurrentInjectedEnvVars(@Nonnull EnvInjectPluginAction envInjectPluginAction) {
        Map<String, String> envVars = envInjectPluginAction.getEnvMap();
        return (envVars) == null ? new HashMap<String, String>() : envVars;
    }

}
