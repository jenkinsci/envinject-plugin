package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gregory Boissinot
 */
@WithJenkins
class EnvInjectEnvVarsEmptyTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void testEmptyVars1() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null,
                "EMPTYVAR1=\n" + "VAR2=VAL2", null, null, false, null);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty(jobPropertyInfo);
        envInjectJobProperty.setOn(true);
        project.addProperty(envInjectJobProperty);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, build.getResult());


        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        assertNotNull(envVars);


        String resultValEnvVar1 = envVars.get("EMPTYVAR1");
        String resultValEnvVar2 = envVars.get("VAR2");
        assertNotNull(resultValEnvVar1);
        assertNotNull(resultValEnvVar2);

        assertEquals(0, resultValEnvVar1.length());
        assertEquals("VAL2", resultValEnvVar2);
    }
}
