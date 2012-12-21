package org.jenkinsci.plugins.envinject;

import hudson.model.*;
import junit.framework.Assert;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEvaluatedGroovyScript extends HudsonTestCase {

    public void testMapGroovyScript() throws Exception {

        FreeStyleProject project = createFreeStyleProject("jobTest");

        StringBuffer groovyScriptContent = new StringBuffer();
        groovyScriptContent.append(
                "            if (CASE==null){\n" +
                        "            return null; \n" +
                        "            } \n" +
                        "\n" +
                        "            def stringValue=\"StRinG\"; \n" +
                        "\n" +
                        "            if (\"upper\".equals(CASE)){ \n" +
                        "            def map = [COMPUTE_VAR: stringValue.toUpperCase()]\n" +
                        "            return map \n" +
                        "            } \n" +
                        "            \n" +
                        "            if (\"lower\".equals(CASE)){ \n" +
                        "            def map = [COMPUTE_VAR: stringValue.toLowerCase()] \n" +
                        "            return map \n" +
                        "            } ");
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, null, null, null, groovyScriptContent.toString(), false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setKeepJenkinsSystemVariables(true);
        envInjectJobProperty.setKeepBuildVariables(true);
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
        project.addProperty(envInjectJobProperty);

        List<ParameterValue> parameterValueList = new ArrayList<ParameterValue>();
        parameterValueList.add(new StringParameterValue("CASE", "lower"));
        ParametersAction parametersAction = new ParametersAction(parameterValueList);

        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);

        String resultValEnvVar = envVars.get("COMPUTE_VAR");
        Assert.assertNotNull(resultValEnvVar);
        Assert.assertEquals("string", resultValEnvVar);
    }


    public void testBuildInVars() throws Exception {

        FreeStyleProject project = createFreeStyleProject("jobTest");

        StringBuffer groovyScriptContent = new StringBuffer();
        groovyScriptContent.append(
                "def env = currentJob.getLastBuild().getEnvironment()\n" +
                        "def buildNumber1 = env['BUILD_NUMBER']\n" +
                        "def buildNumber2 = currentBuild.getNumber()\n" +
                        "def map = [COMPUTE_VAR1: buildNumber1, COMPUTE_VAR2: buildNumber2]\n" +
                        "return map");

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, null, null, null, groovyScriptContent.toString(), false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setKeepJenkinsSystemVariables(true);
        envInjectJobProperty.setKeepBuildVariables(true);
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
        project.addProperty(envInjectJobProperty);

        List<ParameterValue> parameterValueList = new ArrayList<ParameterValue>();
        parameterValueList.add(new StringParameterValue("CASE", "lower"));
        ParametersAction parametersAction = new ParametersAction(parameterValueList);

        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);

        String resultValEnvVar1 = envVars.get("COMPUTE_VAR1");
        String resultValEnvVar2 = envVars.get("COMPUTE_VAR2");
        Assert.assertNotNull(resultValEnvVar1);
        Assert.assertNotNull(resultValEnvVar2);
        Assert.assertEquals(resultValEnvVar2, resultValEnvVar1);
    }
}
