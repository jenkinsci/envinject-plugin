package org.jenkinsci.plugins.envinject;

import com.google.common.collect.ImmutableList;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

public class EnvInjectBuilderTest extends HudsonTestCase {

    public void testBuilderVariablesOverrideParameters() throws Exception {
        FreeStyleProject project = createFreeStyleProject();

        CaptureEnvironmentBuilder captureEnvironmentBuilder = new CaptureEnvironmentBuilder();
        project.getBuildersList().addAll(ImmutableList.of(
                new EnvInjectBuilder(null, "PROP=fromBuilder"),
                new EnvInjectBuilder(null, "NEW_PROP=$PROP"),
                captureEnvironmentBuilder));

        assertBuildStatusSuccess(project.scheduleBuild2(0, new Cause.UserCause(), new ParametersAction(new StringParameterValue("PROP", "fromParameter"))));

        System.out.println(captureEnvironmentBuilder.getEnvVars());
        assertEquals("fromBuilder", captureEnvironmentBuilder.getEnvVars().get("PROP"));
        assertEquals("fromBuilder", captureEnvironmentBuilder.getEnvVars().get("NEW_PROP"));
    }

}
