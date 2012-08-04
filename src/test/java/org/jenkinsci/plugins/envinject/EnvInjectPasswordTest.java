package org.jenkinsci.plugins.envinject;

import hudson.model.*;
import hudson.util.Secret;
import junit.framework.Assert;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordTest extends HudsonTestCase {

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    private FreeStyleProject project;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject();
    }

    public void testEnvInjectPasswordWrapper() throws Exception {

        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });

        project.getBuildWrappersList().add(passwordWrapper);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        checkEnvInjectResult(build);
    }

    public void testEnvInjectJobParameterPassword() throws Exception {

        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        parameterValues.add(new PasswordParameterValue(PWD_KEY, PWD_VALUE));
        ParametersAction parametersAction = new ParametersAction(parameterValues);

        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        checkEnvInjectResult(build);
    }

    private void checkEnvInjectResult(FreeStyleBuild build) {
        EnvInjectAction action = build.getAction(EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();
        //The value must be encrypted in the envVars
        Assert.assertEquals(Secret.fromString(PWD_VALUE).getEncryptedValue(), envVars.get(PWD_KEY));
    }
}
