package org.jenkinsci.plugins.envinject;

import hudson.model.*;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import junit.framework.Assert;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class BuildCauseRetrieverTest extends HudsonTestCase {

    private FreeStyleProject project;

    private static Map<Class<? extends Cause>, String> causeMatchingNames = new HashMap<Class<? extends Cause>, String>();

    static {
        causeMatchingNames.put(Cause.UserCause.class, "MANUALTRIGGER");
        causeMatchingNames.put(SCMTrigger.SCMTriggerCause.class, "SCMTRIGGER");
        causeMatchingNames.put(TimerTrigger.TimerTriggerCause.class, "TIMERTRIGGER");
        causeMatchingNames.put(Cause.UpstreamCause.class, "UPSTREAMTRIGGER");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject();
    }

    public void testManualBuildCause() throws Exception {
        checkCauseArguments(Cause.UserCause.class);
    }

    public void testSCMBuildCause() throws Exception {
        checkCauseArguments(SCMTrigger.SCMTriggerCause.class);
    }

    public void testTIMERBuildCause() throws Exception {
        checkCauseArguments(TimerTrigger.TimerTriggerCause.class);
    }

    public void testUPSTREAMBuildCause() throws Exception {
        FreeStyleProject upProject = createFreeStyleProject();
        FreeStyleBuild upBuild = upProject.scheduleBuild2(0).get();
        Cause.UpstreamCause upstreamCause = new Cause.UpstreamCause((Run) upBuild);
        FreeStyleBuild build = project.scheduleBuild2(0, upstreamCause).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkCauseArguments(upstreamCause);
    }

    public void testCustomBuildCause() throws Exception {
        checkCauseArguments(CustomTestCause.class);
    }

    public void testMultipleBuildCause() throws Exception {

        Cause cause1 = new CustomTestCause();
        Cause cause2 = new SCMTrigger.SCMTriggerCause("TEST");
        CauseAction causeAction = new CauseAction(cause1);
        causeAction.getCauses().add(cause2);

        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), causeAction).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        String customCauseName = CustomTestCause.class.getSimpleName().toUpperCase();
        checkBuildCauses(build, "CUSTOMTESTCAUSE,SCMTRIGGER", "BUILD_CAUSE_" + customCauseName, "BUILD_CAUSE_SCMTRIGGER");
    }

    private void checkCauseArguments(Class<? extends Cause> causeClass) throws Exception {
        checkCauseArguments(causeClass.newInstance());
    }

    private void checkCauseArguments(Cause cause) throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        Assert.assertEquals(Result.SUCCESS, build.getResult());
        checkCauseArgumentsWithBuild(build, cause.getClass());
    }

    private void checkCauseArgumentsWithBuild(FreeStyleBuild build, Class<? extends Cause> causeClass) throws Exception {
        String causeValue = causeMatchingNames.get(causeClass);
        if (causeValue != null) {
            checkBuildCauses(build, causeValue, "BUILD_CAUSE_" + causeValue);
        } else {
            String customCauseName = CustomTestCause.class.getSimpleName().toUpperCase();
            checkBuildCauses(build, customCauseName, "BUILD_CAUSE_" + customCauseName);
        }
    }

    private void checkBuildCauses(FreeStyleBuild build, String expectedMainCauseValue, String... expectedCauseKeys) {

        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);

        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);

        String causeValue = envVars.get("BUILD_CAUSE");
        Assert.assertNotNull(causeValue);
        Assert.assertEquals(expectedMainCauseValue, causeValue);

        for (String causeKey : expectedCauseKeys) {
            Assert.assertEquals("true", envVars.get(causeKey));
        }
    }

}
