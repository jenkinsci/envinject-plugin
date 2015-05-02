package org.jenkinsci.plugins.envinject.service;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Gregory Boissinot
 */
public class BuildCauseRetriever {

    /**
     * Maximum depth of transitive upstream causes we want to record.
     */
    private static final int MAX_UPSTREAM_DEPTH = 10;
    public static final String ENV_CAUSE = "BUILD_CAUSE";
    public static final String ENV_ROOT_CAUSE = "ROOT_BUILD_CAUSE";

    public Map<String, String> getTriggeredCause(AbstractBuild<?, ?> build) {
        CauseAction causeAction = build.getAction(CauseAction.class);
        List<Cause> buildCauses = causeAction.getCauses();
        Map<String, String> env = new HashMap<String, String>();
        List<String> directCauseNames = new ArrayList<String>();
        Set<String> rootCauseNames = new LinkedHashSet<String>();
        for (Cause cause : buildCauses) {
            directCauseNames.add(getTriggerName(cause));
            insertRootCauseNames(rootCauseNames, cause, 0);
        }
        env.putAll(buildCauseEnvironmentVariables(ENV_CAUSE, directCauseNames));
        env.putAll(buildCauseEnvironmentVariables(ENV_ROOT_CAUSE, rootCauseNames));
        return env;
    }

    private static void insertRootCauseNames(Set<String> causeNames, Cause cause, int depth) {
        if (cause instanceof Cause.UpstreamCause) {
            if (depth == MAX_UPSTREAM_DEPTH) {
                causeNames.add("DEEPLYNESTEDCAUSES");
            } else {
                Cause.UpstreamCause c = (Cause.UpstreamCause)cause;
                List<Cause> upstreamCauses = c.getUpstreamCauses();
                for (Cause upstreamCause : upstreamCauses)
                    insertRootCauseNames(causeNames, upstreamCause, depth + 1);
            }
        } else {
            causeNames.add(getTriggerName(cause));
        }
    }

    private static Map<String, String> buildCauseEnvironmentVariables(String envBase, Collection<String> causeNames) {
        Map<String, String> triggerVars = new HashMap<String, String>();
        StringBuilder all = new StringBuilder();
        int count = 0;
        for (String name : causeNames) {
            if (!StringUtils.isBlank(name)) {
                triggerVars.put(envBase + "_" + name, "true");
                if (count > 0) {
                    all.append(",");
                }
                all.append(name);
                count++;
            }
        }
        // add variable containing all the trigger names
        triggerVars.put(envBase, all.toString());
        return triggerVars;
    }

    @SuppressWarnings(value = "deprecation")
    private static String getTriggerName(Cause cause) {
        if (SCMTrigger.SCMTriggerCause.class.isInstance(cause)) {
            return "SCMTRIGGER";
        } else if (TimerTrigger.TimerTriggerCause.class.isInstance(cause)) {
            return "TIMERTRIGGER";
        } else if (Cause.UserIdCause.class.isInstance(cause)) {
            return "MANUALTRIGGER";
        } else if (Cause.UserCause.class.isInstance(cause)) {
            return "MANUALTRIGGER";
        } else if (Cause.UpstreamCause.class.isInstance(cause)) {
            return "UPSTREAMTRIGGER";
        } else if (cause != null) {
            return cause.getClass().getSimpleName().toUpperCase();
        }

        return null;
    }

}
