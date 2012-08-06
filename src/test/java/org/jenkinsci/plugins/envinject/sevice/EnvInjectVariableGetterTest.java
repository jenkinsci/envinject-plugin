package org.jenkinsci.plugins.envinject.sevice;

import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectVariableGetterTest {

    private EnvInjectVariableGetter variableGetter;

    private AbstractBuild build;

    @Before
    public void setUp() {
        variableGetter = new EnvInjectVariableGetter();
        build = mock(AbstractBuild.class);
    }

    private static Map<String, String> envVarsSample1 = new HashMap<String, String>();
    private static Map<String, String> buildEnvVarsSample1 = new HashMap<String, String>();

    static {
        envVarsSample1.put("KEY_ENV1", "VAL1");
        envVarsSample1.put("KEY_ENV2", "VAL2");
        envVarsSample1.put("KEY_ENV3", "VAL3");

        buildEnvVarsSample1.put("KEY_VAR_ENV1", "VAR_VAL1");
        buildEnvVarsSample1.put("KEY_VAR_ENV2", "VAR_VAL2");
        buildEnvVarsSample1.put("KEY_VAR_ENV3", "VAR_VAL3");

    }

    @Test
    public void getEnvVarsPreviousStepsWithEnvInjectAction() throws Exception {
        EnvInjectPluginAction envInjectPluginAction = new EnvInjectPluginAction(build, envVarsSample1);
        when(build.getAction(EnvInjectPluginAction.class)).thenReturn(envInjectPluginAction);
        Map<String, String> envVars = variableGetter.getEnvVarsPreviousSteps(build, mock(EnvInjectLogger.class));
        assertTrue(sameMap(envVarsSample1, envVars));
    }

    @Test
    public void getEnvVarsPreviousStepsWithEnvInjectActionMatrixRun() throws Exception {
        build = mock(MatrixRun.class);
        EnvInjectPluginAction envInjectPluginAction = new EnvInjectPluginAction(build, envVarsSample1);
        when(build.getAction(EnvInjectPluginAction.class)).thenReturn(envInjectPluginAction);
        when(build.getBuildVariables()).thenReturn(buildEnvVarsSample1);
        Map<String, String> resultEnvVars = variableGetter.getEnvVarsPreviousSteps(build, mock(EnvInjectLogger.class));
        Map<String, String> expectedEnvVars = new HashMap<String, String>();
        expectedEnvVars.putAll(envVarsSample1);
        expectedEnvVars.putAll(buildEnvVarsSample1);
        assertTrue(sameMap(expectedEnvVars, resultEnvVars));
    }


    private boolean sameMap(Map<String, String> expectedMap, Map<String, String> actualMap) {

        if (expectedMap == null && actualMap == null) {
            return true;
        }

        if (expectedMap == null) {
            return false;
        }

        if (actualMap == null) {
            return false;
        }

        if (expectedMap.size() != actualMap.size()) {
            return false;
        }

        for (Map.Entry<String, String> entry : actualMap.entrySet()) {
            String expectedValue = expectedMap.get(entry.getKey());
            String actualValue = actualMap.get(entry.getKey());
            if (expectedValue == null && actualValue == null) {
                return true;
            }

            if (expectedValue == null) {
                return false;
            }

            if (!expectedValue.equals(actualValue)) {
                return false;
            }
        }

        return true;
    }

}
