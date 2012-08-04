package org.jenkinsci.plugins.envinject.service;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import junit.framework.Assert;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class BuildCauseRetrieverTest extends HudsonTestCase {

    private FreeStyleProject project;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject();
    }

    public void testManualBuildCause() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause()).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkBuildCauses(build, "MANUALTRIGGER", "BUILD_CAUSE_MANUALTRIGGER");
    }

    public void testSCMBuildCause() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0, new SCMTrigger.SCMTriggerCause("TEST")).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkBuildCauses(build, "SCMTRIGGER", "BUILD_CAUSE_SCMTRIGGER");
    }

    public void testTIMERBuildCause() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0, new TimerTrigger.TimerTriggerCause()).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkBuildCauses(build, "TIMERTRIGGER", "BUILD_CAUSE_TIMERTRIGGER");
    }

    public void testUPSTREAMBuildCause() throws Exception {
        FreeStyleProject upProject = createFreeStyleProject();
        FreeStyleBuild upBuild = upProject.scheduleBuild2(0).get();
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UpstreamCause(upBuild)).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkBuildCauses(build, "UPSTREAMTRIGGER", "BUILD_CAUSE_UPSTREAMTRIGGER");
    }

    public void testCustomBuildCause() throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0, new CustomTestCause()).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        String customCauseName = CustomTestCause.class.getSimpleName().toUpperCase();
        checkBuildCauses(build, customCauseName, "BUILD_CAUSE_" + customCauseName);
    }

    private void checkBuildCauses(FreeStyleBuild build, String buildCauseValue, String... buildCauseKeys) {
        EnvInjectAction action = build.getAction(EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();
        Assert.assertEquals(buildCauseValue, envVars.get("BUILD_CAUSE"));
        for (String buildCauseKey : buildCauseKeys) {
            Assert.assertEquals("true", envVars.get(buildCauseKey));
        }
    }

}
