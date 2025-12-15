package org.jenkinsci.plugins.envinject;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Result;
import hudson.util.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gregory Boissinot
 */
@WithJenkins
class EnvInjectPasswordMatrixProjectTest {

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void testEnvInjectPasswordWrapper() throws Exception {
        MatrixProject project = jenkins.createProject(MatrixProject.class);
        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });

        project.getBuildWrappersList().add(passwordWrapper);
        MatrixBuild matrixBuild = project.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, matrixBuild.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction action = matrixBuild.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();
        //The value must be encrypted in the envVars
        assertEquals(Secret.fromString(PWD_VALUE), Secret.decrypt(envVars.get(PWD_KEY)));
    }
}
