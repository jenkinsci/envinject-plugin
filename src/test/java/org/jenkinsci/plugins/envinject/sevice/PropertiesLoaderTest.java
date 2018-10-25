package org.jenkinsci.plugins.envinject.sevice;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Gregory Boissinot
 */
public class PropertiesLoaderTest {

    PropertiesLoader propertiesLoader = new PropertiesLoader();

    //-- File

    @Test(expected = NullPointerException.class)
    public void nullFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(null, new HashMap<String, String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void notExistFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(new File("not exist"), new HashMap<String, String>());
    }

    @Test
    public void emptyFile() throws Exception {
        File emptyFile = File.createTempFile("test", "test");
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(emptyFile, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(0, gatherVars.size());
    }

    //-- Content

    @Test(expected = NullPointerException.class)
    public void nullContent() throws Exception {
        propertiesLoader.getVarsFromPropertiesContent(null, new HashMap<String, String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyContent() throws Exception {
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(new String(), currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(0, gatherVars.size());
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
        assertEquals(1, gatherVars.size());
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
        assertEquals(3, gatherVars.size());
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
        assertEquals(3, gatherVars.size());
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    @Test
    public void fileWithNewlineInValues() throws Exception {
        checkWithNewlineInValues(true);
    }

    @Test
    public void contentWithNewlineInValues() throws Exception {
        checkWithNewlineInValues(false);
    }

    private void checkWithNewlineInValues(boolean fromFile) throws Exception {
        // Create properties file containing backslash-escaped newlines
        String content = "KEY1=line1\\nline2\nKEY2= line3 \\n\\\nline4 \nKEY3=line5\\\n\\\nline6\nKEY4=line7\\\nline8\\n\\nline9";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(4, gatherVars.size());

        // Values should be trimmed at start & end, otherwise whitespace & newlines should be kept
        assertEquals("line1\nline2", gatherVars.get("KEY1"));
        assertEquals("line3 \nline4", gatherVars.get("KEY2"));
        assertEquals("line5line6", gatherVars.get("KEY3"));
        assertEquals("line7line8\n\nline9", gatherVars.get("KEY4"));
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
        String content = "KEY1 =${VAR1_TO_RESOLVE}\nKEY2=https://github.com\nKEY3=${VAR3_TO_RESOLVE}\\\\otherContent";
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        currentEnvVars.put("VAR1_TO_RESOLVE", "NEW_VALUE1 ");
        currentEnvVars.put("VAR3_TO_RESOLVE", "C:\\Bench");

        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals("NEW_VALUE1", gatherVars.get("KEY1"));
        assertEquals("https://github.com", gatherVars.get("KEY2"));
        assertEquals("C:\\Bench\\otherContent", gatherVars.get("KEY3"));
    }

    //JENKINS-39403
    @Test
    public void fileWithBackSlashes() throws Exception {
        checkWithBackSlashes(true);
    }

    @Test
    public void contentWithBackSlashes() throws Exception {
        checkWithBackSlashes(false);
    }

    private void checkWithBackSlashes(boolean fromFile) throws Exception {
        String content = "KEY1=Test\\Path\\Variable";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(1, gatherVars.size());

        assertEquals("Test\\Path\\Variable", gatherVars.get("KEY1"));
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
