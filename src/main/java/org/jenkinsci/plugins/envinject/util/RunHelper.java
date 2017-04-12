package org.jenkinsci.plugins.envinject.util;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

//TODO: Ideally it should be offered by the core
/**
 * This method contains abstraction layers for methods, which are available only in {@link AbstractBuild}.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class RunHelper {
    
    /**
     * Compatible version of {@link AbstractBuild#getSensitiveBuildVariables()}
     * @param run Run
     * @return List of sensitive variables
     */
    public static Set<String> getSensitiveBuildVariables(@Nonnull Run<?,? > run) {
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
    public static void getBuildVariables(@Nonnull Run<?, ?> run, EnvVars result) {
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
    public static void getJDKVariables(@Nonnull Run<?, ?> run, TaskListener logger, EnvVars result) 
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
}
