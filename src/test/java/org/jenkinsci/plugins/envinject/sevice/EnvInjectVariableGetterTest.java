package org.jenkinsci.plugins.envinject.sevice;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.Assert;

import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Gregory Boissinot
 */
@RunWith(PowerMockRunner.class)
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

    @Test
    @PrepareForTest({ Computer.class, Hudson.class })
    public void testGetJenkinsSystemVariablesForceFetchesGlobalNodesPropertiesFromMaster() throws Exception {

        PowerMockito.mockStatic(Computer.class);
        PowerMockito.mockStatic(Hudson.class);
        Computer computer = mock(Computer.class);
        Node node = mock(Node.class);
        EnvVars envVars = new EnvVars();
        final String PROPERTY_KEY = "PATH";
        final String VALUE_FROM_SLAVE_COMPUTER = "VALUE_FROM_SLAVE_COMPUTER";
        final String VALUE_FROM_GNP_MASTER = "VALUE_FROM_GNP_MASTER";
        envVars.put(PROPERTY_KEY, VALUE_FROM_SLAVE_COMPUTER);
        Hudson hudson = mock(Hudson.class);

        when(Computer.currentComputer()).thenReturn(computer);
        when(computer.getNode()).thenReturn(node);
        when(computer.getEnvironment()).thenReturn(envVars);
        when(node.getAssignedLabels()).thenReturn(new HashSet<LabelAtom>());
        when(computer.getName()).thenReturn("slave0");
        when(Hudson.getInstance()).thenReturn(hudson);
        when(hudson.getRootDir()).thenReturn(new File(""));

        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = new DescribableList<NodeProperty<?>, NodePropertyDescriptor>(
            hudson);
        EnvironmentVariablesNodeProperty property = new EnvironmentVariablesNodeProperty(
            new EnvironmentVariablesNodeProperty.Entry(PROPERTY_KEY, VALUE_FROM_GNP_MASTER));
        globalNodeProperties.add(property);
        when(Hudson.getInstance().getGlobalNodeProperties()).thenReturn(globalNodeProperties);

        Map<String, String> jenkinsSystemVariables = variableGetter.getJenkinsSystemVariables(false);
        Assert.assertNotNull(jenkinsSystemVariables);
        Assert.assertEquals(VALUE_FROM_GNP_MASTER, jenkinsSystemVariables.get(PROPERTY_KEY));
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
