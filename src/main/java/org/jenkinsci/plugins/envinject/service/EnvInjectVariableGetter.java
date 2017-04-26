package org.jenkinsci.plugins.envinject.service;

import hudson.RestrictedSince;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Hudson.MasterComputer;
import hudson.model.Node;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
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
        return RunHelper.getBuildVariables(build, logger);
    }

    @CheckForNull
    @SuppressWarnings("unchecked")
    public EnvInjectJobProperty getEnvInjectJobProperty(@Nonnull AbstractBuild build) {
        return RunHelper.getEnvInjectJobProperty(build);
    }

    @Nonnull
    public Map<String, String> getEnvVarsPreviousSteps(
            @Nonnull AbstractBuild build, @Nonnull EnvInjectLogger logger) 
            throws IOException, InterruptedException, EnvInjectException {
        return RunHelper.getEnvVarsPreviousSteps(build, logger);
    }
}
