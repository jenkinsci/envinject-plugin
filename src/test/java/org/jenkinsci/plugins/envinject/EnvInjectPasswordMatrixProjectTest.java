package org.jenkinsci.plugins.envinject;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Result;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordMatrixProjectTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    @Test
    public void testEnvInjectPasswordWrapper() throws Exception {
        MatrixProject project = jenkins.createMatrixProject();
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
        assertEquals(Secret.fromString(PWD_VALUE).getEncryptedValue(), envVars.get(PWD_KEY));
    }
}
