package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
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
class GlobalPropertiesTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void testGlobalPropertiesWithWORKSPACE() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        final String testWorkspaceVariableName = "TEST_WORKSPACE";
        final String testWorkspaceVariableValue = "${WORKSPACE}";
        final String workspaceName = "WORKSPACE";

        //A global node property TEST_WORKSPACE
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Jenkins.get().getGlobalNodeProperties();
        globalNodeProperties.add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(testWorkspaceVariableName, testWorkspaceVariableValue)));

        EnvInjectJobProperty jobProperty = new EnvInjectJobProperty();
        jobProperty.setOn(true);
        jobProperty.setKeepBuildVariables(true);
        jobProperty.setKeepJenkinsSystemVariables(true);
        project.addProperty(jobProperty);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        org.jenkinsci.lib.envinject.EnvInjectAction action = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();

        String workspaceProcessed = envVars.get(workspaceName);
        assertNotNull(workspaceProcessed);
        String testWorkspaceProcessed = envVars.get(testWorkspaceVariableName);
        assertNotNull(testWorkspaceProcessed);

        assertEquals(workspaceProcessed, testWorkspaceProcessed);
    }

    /**
     * Specific use case: We set a global workspace at job level
     */
    @Test
    void testGlobalPropertiesSetWORKSPACE() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        final String testGlobalVariableName = "WORKSPACE";
        final String testGlobalVariableValue = "WORKSPACE_VALUE";
        final String testJobVariableName = "TEST_JOB_WORKSPACE";
        final String testJobVariableExprValue = "${WORKSPACE}";

        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Jenkins.get().getGlobalNodeProperties();
        globalNodeProperties.add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(testGlobalVariableName, testGlobalVariableValue)));

        EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null,
                testJobVariableName + "=" + testJobVariableExprValue, null, null, true, null);
        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper(info);
        project.getBuildWrappersList().add(envInjectBuildWrapper);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        org.jenkinsci.lib.envinject.EnvInjectAction action = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();

        String result_testGlobalVariableName = envVars.get(testGlobalVariableName);
        assertNotNull(result_testGlobalVariableName);
        String result_testJobVariableName = envVars.get(testJobVariableName);
        assertNotNull(result_testJobVariableName);

        assertEquals(result_testGlobalVariableName, result_testJobVariableName);
    }
}
