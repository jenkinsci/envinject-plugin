package org.jenkinsci.plugins.envinject.service;

import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Joiner.on;
import hudson.model.Run;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import static org.apache.commons.lang.StringUtils.isNotBlank;


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

    @Nonnull
    public Map<String, String> getTriggeredCause(@Nonnull Run<?, ?> run) {
        CauseAction causeAction = run.getAction(CauseAction.class);
        Map<String, String> env = new HashMap<String, String>();
        List<String> directCauseNames = new ArrayList<String>();
        Set<String> rootCauseNames = new LinkedHashSet<String>();

        if (causeAction != null) {
            List<Cause> buildCauses = causeAction.getCauses();
            for (Cause cause : buildCauses) {
                directCauseNames.add(getTriggerName(cause));
                insertRootCauseNames(rootCauseNames, cause, 0);
            }
        } else {
            directCauseNames.add("UNKNOWN");
            rootCauseNames.add("UNKNOWN");
        }
        env.putAll(buildCauseEnvironmentVariables(ENV_CAUSE, directCauseNames));
        env.putAll(buildCauseEnvironmentVariables(ENV_ROOT_CAUSE, rootCauseNames));
        return env;
    }

    /**
     * Inserts root cause names to the specified target container.
     * @param causeNamesTarget Target set. May receive null items
     * @param cause Cause to be added. For {@code Cause.UstreamCause} there will be in-depth search
     * @param depth Current search depth. {@link #MAX_UPSTREAM_DEPTH} is a limit
     */
    private static void insertRootCauseNames(@Nonnull Set<String> causeNamesTarget, @CheckForNull Cause cause, int depth) {
        if (cause instanceof Cause.UpstreamCause) {
            if (depth == MAX_UPSTREAM_DEPTH) {
                causeNamesTarget.add("DEEPLYNESTEDCAUSES");
            } else {
                Cause.UpstreamCause c = (Cause.UpstreamCause) cause;
                List<Cause> upstreamCauses = c.getUpstreamCauses();
                for (Cause upstreamCause : upstreamCauses)
                    insertRootCauseNames(causeNamesTarget, upstreamCause, depth + 1);
            }
        } else {
            //TODO: Accordig to the current design this list may receive null for unknown trigger. Bug?
            // Should actually return UNKNOWN
            causeNamesTarget.add(getTriggerName(cause));
        }
    }

    private static Map<String, String> buildCauseEnvironmentVariables(String envBase, Collection<String> causeNames) {
        Map<String, String> triggerVars = new HashMap<String, String>();
        List<String> nonEmptyNames = new ArrayList<String>();
        for (String name : causeNames) {
            if (isNotBlank(name)) {
                triggerVars.put(on("_").join(envBase, name), "true");
                nonEmptyNames.add(name);
            }
        }
        // add variable containing all the trigger names
        triggerVars.put(envBase, on(",").join(nonEmptyNames));
        return triggerVars;
    }

    @CheckForNull
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
            return cause.getClass().getSimpleName().toUpperCase(Locale.ENGLISH);
        }

        return null;
    }

}
