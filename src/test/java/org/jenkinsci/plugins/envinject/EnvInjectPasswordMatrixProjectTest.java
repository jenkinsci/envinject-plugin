package org.jenkinsci.plugins.envinject;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Result;
import hudson.util.Secret;
import junit.framework.Assert;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordMatrixProjectTest extends HudsonTestCase {

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    private MatrixProject project;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createMatrixProject();
    }

    public void testEnvInjectPasswordWrapper() throws Exception {

        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });

        project.getBuildWrappersList().add(passwordWrapper);
        MatrixBuild matrixBuild = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, matrixBuild.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction action = matrixBuild.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();
        //The value must be encrypted in the envVars
        Assert.assertEquals(Secret.fromString(PWD_VALUE).getEncryptedValue(), envVars.get(PWD_KEY));

    }

}
