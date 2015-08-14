package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EnvInjectEnvVarsContributorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void envVarsJob() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, "REPO=trivial-maven", null, null, null, false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
        project.addProperty(envInjectJobProperty);

        TaskListener listener = jenkins.createTaskListener();
        EnvVars environment = project.getEnvironment(jenkins.getInstance(), listener);
        assertNotNull(environment.get("REPO"));
    }

    @Test
    public void notAvailableEnvVarsJob() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("notAvailableEnvVarsJob");

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, "VAR1=${WORKSPACE}\nVAR2=${JOB_NAME}", null, null, null, false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty();
        envInjectJobProperty.setOn(true);
        envInjectJobProperty.setInfo(jobPropertyInfo);
        project.addProperty(envInjectJobProperty);

        TaskListener listener = jenkins.createTaskListener();
        EnvVars environment = project.getEnvironment(jenkins.getInstance(), listener);
        assertEquals("${WORKSPACE}", environment.get("VAR1"));
        assertEquals("notAvailableEnvVarsJob", environment.get("VAR2"));
    }
}
