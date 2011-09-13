package org.jenkinsci.plugins.envinject.service;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class BuildCauseRetriever {

    public static final String ENV_CAUSE = "BUILD_CAUSE";

    public Map<String, String> getTriggeredCause(AbstractBuild<?, ?> build) {

        Map<String, String> triggerVars = new HashMap<String, String>();
        StringBuilder all = new StringBuilder();
        CauseAction causeAction = build.getAction(CauseAction.class);
        List<Cause> buildCauses = causeAction.getCauses();
        for (Cause cause : buildCauses) {
            String name = getTriggerName(cause);
            if (!StringUtils.isBlank(name)) {
                triggerVars.put(ENV_CAUSE + "_" + name, "true");
                all.append(",");
                all.append(name);
            }
        }
        // add variable containing all the trigger names
        triggerVars.put(ENV_CAUSE, all.toString().substring(1));
        return triggerVars;
    }


    private static String getTriggerName(Cause cause) {
        if (SCMTrigger.SCMTriggerCause.class.isInstance(cause)) {
            return "SCMTRIGGER";
        } else if (TimerTrigger.TimerTriggerCause.class.isInstance(cause)) {
            return "TIMERTRIGGER";
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
