package org.jenkinsci.plugins.envinject.sevice;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

/**
 * @author Gregory Boissinot
 */
public class PropertiesLoaderTest {

    PropertiesLoader propertiesLoader = new PropertiesLoader();

    //-- File

    @Test(expected = NullPointerException.class)
    public void nullFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(null, any(Map.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void notExistFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(new File("not exist"), any(Map.class));
    }

    @Test
    public void emptyFile() throws Exception {
        File emptyFile = File.createTempFile("test", "test");
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(emptyFile, currentEnvVars);
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 0);
    }

    //-- Content

    @Test(expected = NullPointerException.class)
    public void nullContent() throws Exception {
        propertiesLoader.getVarsFromPropertiesContent(null, any(Map.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyContent() throws Exception {
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(new String(), currentEnvVars);
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 0);
    }

    //--Both

    @Test
    public void fileWithOneElement() throws Exception {
        checkWithOneElement(true);
    }

    @Test
    public void contentWithOneElement() throws Exception {
        checkWithOneElement(false);
    }

    private void checkWithOneElement(boolean fromFile) throws Exception {
        String content = "SOMEKEY=SOMEVALUE";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 1);
        assertEquals("SOMEVALUE", gatherVars.get("SOMEKEY"));
    }

    @Test
    public void fileWithThreeElements() throws Exception {
        checkWithThreeElements(true);
    }

    @Test
    public void contentWithThreeElements() throws Exception {
        checkWithThreeElements(false);
    }

    private void checkWithThreeElements(boolean fromFile) throws Exception {
        String content = "KEY1=VALUE1\nKEY2=VALUE2\nKEY3=VALUE3";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 3);
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    @Test
    public void fileWithSpaceInElements() throws Exception {
        checkWithSpaceInElements(true);
    }

    @Test
    public void contentWithSpaceInElements() throws Exception {
        checkWithSpaceInElements(false);
    }

    private void checkWithSpaceInElements(boolean fromFile) throws Exception {
        String content = "KEY1 =VALUE1\nKEY2=VALUE2\nKEY3=VALUE3 ";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 3);
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    @Test
    public void fileWithVarsToResolve() throws Exception {
        checkWithVarsToResolve(true);
    }

    @Test
    public void contentWithVarsToResolve() throws Exception {
        checkWithVarsToResolve(false);
    }

    private void checkWithVarsToResolve(boolean fromFile) throws Exception {
        String content = "KEY1 =${VAR1_TO_RESOLVE}\nKEY2=VALUE2\nKEY3=${VAR3_TO_RESOLVE}\\otherContent";
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        currentEnvVars.put("VAR1_TO_RESOLVE", "NEW_VALUE1");
        currentEnvVars.put("VAR3_TO_RESOLVE", "NEW_VALUE3");

        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, currentEnvVars);
        assertNotNull(gatherVars);
        assertTrue(gatherVars.size() == 3);
        assertEquals("NEW_VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("NEW_VALUE3\\otherContent", gatherVars.get("KEY3"));
    }

    private Map<String, String> gatherEnvVars(boolean fromFile, String content2Load, Map<String, String> currentEnvVars) throws Exception {
        File propFile = File.createTempFile("test", "test");
        FileUtils.writeStringToFile(propFile, content2Load);
        if (fromFile) {
            return propertiesLoader.getVarsFromPropertiesFile(propFile, currentEnvVars);
        } else {
            return propertiesLoader.getVarsFromPropertiesContent(content2Load, currentEnvVars);
        }
    }

}
