package org.jenkinsci.plugins.envinject;

import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static com.google.common.base.Joiner.on;
import static hudson.model.Result.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.withCause;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.withCausesIsTrue;

/**
 * @author Gregory Boissinot
 */
@SuppressWarnings("deprecation")
@WithJenkins
class BuildCauseRetrieverTest {

    public static final String BUILD_CAUSE = "BUILD_CAUSE";
    public static final String ROOT_BUILD_CAUSE = "ROOT_BUILD_CAUSE";

    public static final String MANUAL_TRIGGER = "MANUALTRIGGER";
    public static final String SCM_TRIGGER = "SCMTRIGGER";
    public static final String TIMER_TRIGGER = "TIMERTRIGGER";
    public static final String UPSTREAM_TRIGGER = "UPSTREAMTRIGGER";

    private static JenkinsRule jenkins;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void shouldWriteInfoAboutManualBuildCause() throws Exception {
        Cause cause = Cause.UserCause.class.newInstance();
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, cause).get();

        assertThat(build.getResult(), is(SUCCESS));
        assertThat(build, withCause(BUILD_CAUSE, MANUAL_TRIGGER));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, MANUAL_TRIGGER));
        assertThat(build, withCausesIsTrue(sub(BUILD_CAUSE, MANUAL_TRIGGER), sub(ROOT_BUILD_CAUSE, MANUAL_TRIGGER)));
    }

    @Test
    void shouldWriteInfoAboutSCMBuildCause() throws Exception {
        Cause cause = SCMTrigger.SCMTriggerCause.class.newInstance();
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, cause).get();

        assertThat(build.getResult(), is(SUCCESS));
        assertThat(build, withCause(BUILD_CAUSE, SCM_TRIGGER));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, SCM_TRIGGER));
        assertThat(build, withCausesIsTrue(sub(BUILD_CAUSE, SCM_TRIGGER), sub(ROOT_BUILD_CAUSE, SCM_TRIGGER)));
    }

    @Test
    void shouldWriteInfoAboutTimerBuildCause() throws Exception {
        Cause cause = TimerTrigger.TimerTriggerCause.class.newInstance();
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, cause).get();

        assertThat(build.getResult(), is(SUCCESS));

        assertThat(build, withCause(BUILD_CAUSE, TIMER_TRIGGER));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, TIMER_TRIGGER));
        assertThat(build, withCausesIsTrue(sub(BUILD_CAUSE, TIMER_TRIGGER), sub(ROOT_BUILD_CAUSE, TIMER_TRIGGER)));
    }

    @Test
    void shouldWriteInfoAboutUpstreamBuildCause() throws Exception {
        FreeStyleProject upProject = jenkins.createFreeStyleProject();
        FreeStyleBuild upBuild = upProject.scheduleBuild2(0, new Cause.UserCause()).get();

        Cause.UpstreamCause upstreamCause = new Cause.UpstreamCause((Run) upBuild);
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, upstreamCause).get();

        assertThat(build.getResult(), is(SUCCESS));

        assertThat(build, withCause(BUILD_CAUSE, UPSTREAM_TRIGGER));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, MANUAL_TRIGGER));
        assertThat(build, withCausesIsTrue(sub(BUILD_CAUSE, UPSTREAM_TRIGGER), sub(ROOT_BUILD_CAUSE, MANUAL_TRIGGER)));
    }

    @Test
    void shouldWriteInfoAboutCustomBuildCause() throws Exception {
        Cause cause = CustomTestCause.class.newInstance();
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, cause).get();

        assertThat(build.getResult(), is(SUCCESS));

        String customCauseName = CustomTestCause.class.getSimpleName().toUpperCase();

        assertThat(build, withCause(BUILD_CAUSE, customCauseName));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, customCauseName));
        assertThat(build, withCausesIsTrue(sub(BUILD_CAUSE, customCauseName), sub(ROOT_BUILD_CAUSE, customCauseName)));
    }

    @Test
    void shouldWriteInfoAboutMultipleBuildCauses() throws Exception {
        Cause cause1 = new CustomTestCause();
        Cause cause2 = new SCMTrigger.SCMTriggerCause("TEST");
        CauseAction causeAction = new CauseAction(cause1, cause2);

        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0,
                new Cause.UserCause(), causeAction).get();
        assertThat(build.getResult(), is(SUCCESS));

        String customCauseName = CustomTestCause.class.getSimpleName().toUpperCase();

        assertThat(build, withCause(BUILD_CAUSE, on(",").join("CUSTOMTESTCAUSE", SCM_TRIGGER)));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, on(",").join("CUSTOMTESTCAUSE", SCM_TRIGGER)));
        assertThat(build, withCausesIsTrue(
                        sub(BUILD_CAUSE, customCauseName),
                        sub(BUILD_CAUSE, SCM_TRIGGER),
                        sub(ROOT_BUILD_CAUSE, customCauseName),
                        sub(ROOT_BUILD_CAUSE, SCM_TRIGGER))
        );
    }

    @Test
    @Issue("28188")
    void shouldWriteInfoAboutAnonymousClassCause() throws Exception {
        FreeStyleBuild build = jenkins.createFreeStyleProject().scheduleBuild2(0, new Cause() {
            @Override
            public String getShortDescription() {
                return "This build was started by a hobbit Bilbo. Bilbo Baggins";
            }
        }).get();

        assertThat(build.getResult(), is(SUCCESS));

        assertThat(build, withCause(BUILD_CAUSE, ""));
        assertThat(build, withCause(ROOT_BUILD_CAUSE, ""));
    }

    private String sub(String first, String second) {
        return on("_").join(first, second);
    }
}
