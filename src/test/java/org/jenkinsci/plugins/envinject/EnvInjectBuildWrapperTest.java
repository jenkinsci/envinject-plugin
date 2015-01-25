package org.jenkinsci.plugins.envinject;

import static org.junit.Assert.assertEquals;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import junit.framework.Assert;

import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvInjectBuildWrapperTest {

    public @Rule JenkinsRule j = new JenkinsRule();

    @Test
    public void injectText() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "FILE_VAR=fvalue"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "TEXT_VAR=tvalue", null, null, null, false
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("tvalue", capture.getEnvVars().get("TEXT_VAR"));
        assertEquals("fvalue", capture.getEnvVars().get("FILE_VAR"));
    }

    @Test
    public void injectFromScript() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.groovy", "return ['FILE_VAR': 'fvalue']"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                null, null, null, null, "return ['GROOVY_VAR': 'gvalue']", false
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("gvalue", capture.getEnvVars().get("GROOVY_VAR"));
    }

    @Test
    public void testPropertiesContentCustomWorkspace() throws Exception {

        String customWorkspaceValue = Hudson.getInstance().getRootPath().getRemote() + "/customWorkspace";
        String customEnvVarName = "materialize_workspace_path";
        String customEnvVarValue = "${WORKSPACE}/materialize_workspace";

        FreeStyleProject project = j.createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

        String propertiesContent = customEnvVarName + "=" + customEnvVarValue;

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, propertiesContent, null, null, null, false);
        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper();
        envInjectBuildWrapper.setInfo(jobPropertyInfo);
        project.getBuildWrappersList().add(envInjectBuildWrapper);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        //1-- Compute expected injected var value
        //Retrieve build workspace
        String buildWorkspaceValue = build.getWorkspace().getRemote();
        Assert.assertEquals(customWorkspaceValue, buildWorkspaceValue);
        //Compute value with workspace
        Map<String, String> mapEnvVars = new HashMap<String, String>();
        mapEnvVars.put("WORKSPACE", buildWorkspaceValue);
        String expectedCustomEnvVarValue = resolveVars(customEnvVarValue, mapEnvVars);

        //2-- Get injected value for the specific variable
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);
        String resolvedValue = envVars.get(customEnvVarName);
        Assert.assertNotNull(resolvedValue);

        //3-- Test equals
        Assert.assertEquals(expectedCustomEnvVarValue, resolvedValue);
    }

    private String resolveVars(String value, Map<String, String> map) {
        return Util.replaceMacro(value, map);
    }
}
