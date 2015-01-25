package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import junit.framework.Assert;

import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEnvVarsEmpty extends HudsonTestCase {


    public void testEmptyVars1() throws Exception {

        FreeStyleProject project = createFreeStyleProject();
        StringBuffer propertiesContent = new StringBuffer("EMPTYVAR1=\n");
        propertiesContent.append("VAR2=VAL2");
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, propertiesContent.toString(), null, null, null, false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
        project.addProperty(envInjectJobProperty);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());


        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);


        String resultValEnvVar1 = envVars.get("EMPTYVAR1");
        String resultValEnvVar2 = envVars.get("VAR2");
        Assert.assertNotNull(resultValEnvVar1);
        Assert.assertNotNull(resultValEnvVar2);

        Assert.assertEquals(0, resultValEnvVar1.length());
        Assert.assertEquals("VAL2", resultValEnvVar2);
    }
}
