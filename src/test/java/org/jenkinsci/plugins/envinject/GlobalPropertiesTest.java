package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import junit.framework.Assert;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class GlobalPropertiesTest extends HudsonTestCase {


    private FreeStyleProject project;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject();
    }


    public void testGlobalPropertiesWithWORKSPACE() throws Exception {

        final String testWorkspaceVariableName = "TEST_WORKSPACE";
        final String testWorkspaceVariableValue = "${WORKSPACE}";
        final String workspaceName = "WORKSPACE";

        //A global node property TEST_WORKSPACE
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Hudson.getInstance().getGlobalNodeProperties();
        globalNodeProperties.add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(testWorkspaceVariableName, testWorkspaceVariableValue)));

        EnvInjectJobProperty jobProperty = new EnvInjectJobProperty();
        jobProperty.setOn(true);
        jobProperty.setKeepBuildVariables(true);
        jobProperty.setKeepJenkinsSystemVariables(true);
        project.addProperty(jobProperty);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction action = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();

        String workspaceProcessed = envVars.get(workspaceName);
        assertNotNull(workspaceProcessed);
        String testWorkspaceProcessed = envVars.get(testWorkspaceVariableName);
        assertNotNull(testWorkspaceProcessed);

        assertEquals(workspaceProcessed, testWorkspaceProcessed);
    }
}
