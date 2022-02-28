package org.jenkinsci.plugins.envinject.util;

import hudson.EnvVars;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Environment;
import hudson.model.EnvironmentContributor;
import hudson.model.Executor;
import hudson.model.JDK;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;
import org.jenkinsci.plugins.envinject.service.BuildCauseRetriever;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

//TODO: Ideally it should be offered by the core
/**
 * This method contains abstraction layers for methods, which are available only in {@link AbstractBuild}.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class RunHelper {
    
    private static final Logger LOGGER = Logger.getLogger(RunHelper.class.getName());
    
    /**
     * Compatible version of {@link AbstractBuild#getSensitiveBuildVariables()}
     * @param run Run
     * @return List of sensitive variables
     */
    public static Set<String> getSensitiveBuildVariables(@NonNull Run<?,? > run) {
        if (run instanceof AbstractBuild) {
            return ((AbstractBuild)run).getSensitiveBuildVariables();
        }
        
        Set<String> s = new HashSet<String>();
        ParametersAction parameters = run.getAction(ParametersAction.class);
        if (parameters != null) {
            for (ParameterValue p : parameters) {
                if (p.isSensitive()) {
                    s.add(p.getName());
                }
            }
        }
        
        return s;
    }
    
    /**
     * Gets build variables.
     * For {@link AbstractBuild} it invokes the standard method, 
     * for other types it relies on {@link ParametersAction} only.
     * @param run Run
     * @param result Target collection, where the variables will be added
     */
    public static void getBuildVariables(@NonNull Run<?, ?> run, EnvVars result) {
        if (run instanceof AbstractBuild) {
            Map buildVariables = ((AbstractBuild)run).getBuildVariables();
            result.putAll(buildVariables);
        }
           
        ParametersAction parameters = run.getAction(ParametersAction.class);
        if (parameters!=null) {
            // TODO: not sure if buildEnvironment is safe in this context (e.g. FileParameter)
            for (ParameterValue p : parameters) {
                p.buildEnvironment(run, result);
            }
        }
    }
    
    /**
     * Gets JDK variables.
     * For {@link AbstractBuild} it invokes operation on the node to retrieve the data; 
     * for other types it does nothing.
     * @param run Run
     * @param logger Logger
     * @param result Target collection, where the variables will be added
     * @throws IOException Operation failure
     * @throws InterruptedException Operation has been interrupted
     */
    public static void getJDKVariables(@NonNull Run<?, ?> run, TaskListener logger, EnvVars result) 
            throws IOException, InterruptedException {
        if (run instanceof AbstractBuild) {
            AbstractBuild b = (AbstractBuild) run;
            JDK jdk = b.getProject().getJDK();
            if (jdk != null) {
                Node node = b.getBuiltOn();
                if (node != null) {
                    jdk = jdk.forNode(node, logger);
                }
                jdk.buildEnvVars(result);
            }
        }
    }
    
    // Moved from EnvInjectVariableGetter
    
    public static Map<String, String> getBuildVariables(@NonNull Run<?, ?> run, @NonNull EnvInjectLogger logger) throws EnvInjectException {
        EnvVars result = new EnvVars();

        //Add build process variables
        result.putAll(run.getCharacteristicEnvVars());

        try {
            EnvVars envVars = new EnvVars();
            for (EnvironmentContributor ec : EnvironmentContributor.all()) {
                ec.buildEnvironmentFor(run, envVars, new LogTaskListener(LOGGER, Level.ALL));
                result.putAll(envVars);
            }
            
            // Handle JDK
            RunHelper.getJDKVariables(run, logger.getListener(), result);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }

        Executor e = run.getExecutor();
        if (e != null) {
            result.put("EXECUTOR_NUMBER", String.valueOf(e.getNumber()));
        }

        String rootUrl = Jenkins.getActiveInstance().getRootUrl();
        if (rootUrl != null) {
            result.put("BUILD_URL", rootUrl + run.getUrl());
            result.put("JOB_URL", rootUrl + run.getParent().getUrl());
        }

        //Add build variables such as parameters, plugins contributions, ...
        RunHelper.getBuildVariables(run, result);

        //Retrieve triggered cause
        Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(run);
        result.putAll(triggerVariable);

        return result;
    }

    @CheckForNull
    public static EnvInjectJobProperty getEnvInjectJobProperty(@NonNull Run<?, ?> build) {
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

    @NonNull
    public static Map<String, String> getEnvVarsPreviousSteps(
            @NonNull Run<?, ?> build, @NonNull EnvInjectLogger logger) 
            throws IOException, InterruptedException, EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        // Env vars contributed by build wrappers; no replacement in Pipeline
        if (build instanceof AbstractBuild) {
            List<Environment> environmentList = ((AbstractBuild)build).getEnvironments();
            if (environmentList != null) {
                for (Environment e : environmentList) {
                    if (e != null) {
                        e.buildEnvVars(result);
                    }
                }
            }
        }

        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            result.putAll(getCurrentInjectedEnvVars(envInjectAction));
            //Add build variables with axis for a MatrixRun
            if (build instanceof MatrixRun) {
                result.putAll(((MatrixRun)build).getBuildVariables());
            }
        } else {
            result.putAll(EnvInjectVariableGetter.getJenkinsSystemEnvVars(false));
            result.putAll(getBuildVariables(build, logger));
        }
        return result;
    }

    @NonNull
    private static Map<String, String> getCurrentInjectedEnvVars(@NonNull EnvInjectPluginAction envInjectPluginAction) {
        Map<String, String> envVars = envInjectPluginAction.getEnvMap();
        return (envVars) == null ? new HashMap<String, String>() : envVars;
    }
}
