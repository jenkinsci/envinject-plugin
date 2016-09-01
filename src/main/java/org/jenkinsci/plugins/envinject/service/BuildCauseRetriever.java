package org.jenkinsci.plugins.envinject.service;

import static com.google.common.base.Joiner.*;
import static org.apache.commons.lang.StringUtils.*;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.CauseAction;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    public static final String ENV_USER_NAME = "USER_NAME";
    public static final String ENV_USER_ID = "USER_ID";

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
        env.put(ENV_USER_NAME, getCauseUserInfo(build).get(ENV_USER_NAME));
        env.put(ENV_USER_ID, getCauseUserInfo(build).get(ENV_USER_ID));
        return env;
    }

    private static void insertRootCauseNames(Set<String> causeNames, Cause cause, int depth) {
        if (cause instanceof Cause.UpstreamCause) {
            if (depth == MAX_UPSTREAM_DEPTH) {
                causeNames.add("DEEPLYNESTEDCAUSES");
            } else {
                Cause.UpstreamCause c = (Cause.UpstreamCause) cause;
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

    public Map<String,String> getCauseUserInfo(Run<?, ?> build) {
    	CauseAction causeAction = build.getAction(CauseAction.class);
    	List<Cause> buildCauses = causeAction.getCauses();
    	Map<String,String> userInfo = new HashMap<String,String>();
    	String userName = "";
    	String userId = "";
    	for (Cause cause : buildCauses) {
    		if (isUserCause(cause)) {
    			userName = getUserName(cause);
    			userId=getUserId(cause);
    			break;
    		}
    	}
		userInfo.put(ENV_USER_NAME, userName);
		userInfo.put(ENV_USER_ID, userId);
    	return userInfo;
    }

	@SuppressWarnings("deprecation")
	private static Boolean isUserCause(Cause cause) {
		if (cause instanceof UserCause || cause instanceof UserIdCause){
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private static String getUserName(Cause cause) {
		   	if (cause instanceof UserIdCause) {
		   		Cause.UserIdCause userIdCause = (UserIdCause) cause;
		   		return userIdCause.getUserName();
		   	} else if (cause instanceof UserCause) {
		   		Cause.UserCause userCause = (UserCause) cause;
		   		return userCause.getUserName();
		   	}
		   	return "";
	}

	private static String getUserId(Cause cause) {
		   	if (cause instanceof UserIdCause) {
		   		Cause.UserIdCause userIdCause = (UserIdCause) cause;
		   		return userIdCause.getUserId();
		   	}
		   	return "";
	}

}
