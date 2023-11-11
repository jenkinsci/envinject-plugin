package org.jenkinsci.plugins.envinject.service;

import hudson.RestrictedSince;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Node;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import jenkins.model.Jenkins.MasterComputer;
import org.jenkinsci.plugins.envinject.util.RunHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Utility class for retrieving environment variables.
 * @author Gregory Boissinot
 */
@Restricted(NoExternalUse.class)
@RestrictedSince("2.1")
public class EnvInjectVariableGetter {

    private static Logger LOG = Logger.getLogger(EnvInjectVariableGetter.class.getName());

    @Deprecated
    public EnvInjectVariableGetter() {
    }

    /**
     * @deprecated Use {@link #getJenkinsSystemEnvVars(boolean)}
     */
    @NonNull
    @Deprecated
    public Map<String, String> getJenkinsSystemVariables(boolean forceOnMaster) throws IOException, InterruptedException {
        return getJenkinsSystemEnvVars(forceOnMaster);
    }
    
    //TODO: Move to Another utility class in EnvInject API 
    @NonNull
    public static Map<String, String> getJenkinsSystemEnvVars(boolean forceOnMaster) throws IOException, InterruptedException {
        Map<String, String> result = new TreeMap<String, String>();

        final Computer computer;
        final Jenkins jenkins = Jenkins.get();
        if (forceOnMaster) {
            
            computer = jenkins.toComputer();
        } else {
            computer = Computer.currentComputer();
        }

        //test if there is at least one executor
        if (computer != null) {
            result = computer.getEnvironment().overrideAll(result);
            if(computer instanceof MasterComputer) {
                result.put("NODE_NAME", Jenkins.get().getSelfLabel().getName());
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


    /**
     * @deprecated Use {@link RunHelper#getBuildVariables(hudson.model.Run, hudson.EnvVars)}
     */
    @Deprecated
    public Map<String, String> getBuildVariables(@NonNull AbstractBuild build, @NonNull EnvInjectLogger logger) throws EnvInjectException {
        return RunHelper.getBuildVariables(build, logger);
    }

    /**
     * @deprecated Use {@link RunHelper#getEnvInjectJobProperty(hudson.model.Run)}
     */
    @CheckForNull
    @Deprecated
    public EnvInjectJobProperty getEnvInjectJobProperty(@NonNull AbstractBuild build) {
        return RunHelper.getEnvInjectJobProperty(build);
    }

    /**
     * @deprecated Use {@link RunHelper#getEnvVarsPreviousSteps(hudson.model.Run, org.jenkinsci.lib.envinject.EnvInjectLogger)}
     */
    @NonNull
    @Deprecated
    public Map<String, String> getEnvVarsPreviousSteps(
            @NonNull AbstractBuild build, @NonNull EnvInjectLogger logger) 
            throws IOException, InterruptedException, EnvInjectException {
        return RunHelper.getEnvVarsPreviousSteps(build, logger);
    }
}
