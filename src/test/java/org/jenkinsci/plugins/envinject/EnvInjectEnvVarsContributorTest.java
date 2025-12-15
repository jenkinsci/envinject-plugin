package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("rawtypes")
@WithJenkins
class EnvInjectEnvVarsContributorTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void envVarsJob() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, "REPO=trivial-maven", null, null, false, null);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty(jobPropertyInfo);
        envInjectJobProperty.setOn(true);
        project.addProperty(envInjectJobProperty);

        TaskListener listener = jenkins.createTaskListener();
        EnvVars environment = project.getEnvironment(jenkins.getInstance(), listener);
        assertNotNull(environment.get("REPO"));
    }

    public void notAvailableEnvVarsJob() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("notAvailableEnvVarsJob");

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, "VAR1=${WORKSPACE}\nVAR2=${JOB_NAME}", null, null, false, null);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty(jobPropertyInfo);
        envInjectJobProperty.setOn(true);
        project.addProperty(envInjectJobProperty);

        TaskListener listener = jenkins.createTaskListener();
        EnvVars environment = project.getEnvironment(jenkins.getInstance(), listener);
        assertEquals("${WORKSPACE}", environment.get("VAR1"));
        assertEquals("notAvailableEnvVarsJob", environment.get("VAR2"));
    }
}
