package org.jenkinsci.plugins.envinject;

import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import junit.framework.Assert;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuildWrapperTest extends HudsonTestCase {

    public void testPropertiesContentCustomWorkspace() throws Exception {

        String customWorkspaceValue = Hudson.getInstance().getRootPath().getRemote() + "/customWorkspace";
        String customEnvVarName = "materialize_workspace_path";
        String customEnvVarValue = "${WORKSPACE}/materialize_workspace";

        FreeStyleProject project = createFreeStyleProject();
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
