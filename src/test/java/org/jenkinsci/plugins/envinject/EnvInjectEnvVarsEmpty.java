package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEnvVarsEmpty {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testEmptyVars1() throws Exception {

        FreeStyleProject project = jenkins.createFreeStyleProject();
        StringBuffer propertiesContent = new StringBuffer("EMPTYVAR1=\n");
        propertiesContent.append("VAR2=VAL2");
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, propertiesContent.toString(), null, null, null, false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
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
