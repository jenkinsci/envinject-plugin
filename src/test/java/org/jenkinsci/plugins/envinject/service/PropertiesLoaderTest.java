package org.jenkinsci.plugins.envinject.service;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Gregory Boissinot
 */
class PropertiesLoaderTest {

    final PropertiesLoader propertiesLoader = new PropertiesLoader();

    //-- File

    @Test
    void nullFile() {
        assertThrows(NullPointerException.class, () ->
                propertiesLoader.getVarsFromPropertiesFile(null, new HashMap<>()));
    }

    @Test
    void notExistFile() {
        assertThrows(IllegalArgumentException.class, () ->
                propertiesLoader.getVarsFromPropertiesFile(new File("not exist"), new HashMap<>()));
    }

    @Test
    void emptyFile() throws Exception {
        File emptyFile = File.createTempFile("test", "test");
        Map<String, String> currentEnvVars = new HashMap<>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(emptyFile, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(0, gatherVars.size());
    }

    //-- Content

    @Test
    void nullContent() {
        assertThrows(NullPointerException.class, () ->
                propertiesLoader.getVarsFromPropertiesContent(null, new HashMap<>()));
    }

    @Test
    void emptyContent() {
        Map<String, String> currentEnvVars = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> propertiesLoader.getVarsFromPropertiesContent("", currentEnvVars));
    }

    //--Both

    @Test
    void fileWithOneElement() throws Exception {
        checkWithOneElement(true);
    }

    @Test
    void contentWithOneElement() throws Exception {
        checkWithOneElement(false);
    }

    private void checkWithOneElement(boolean fromFile) throws Exception {
        String content = "SOMEKEY=SOMEVALUE";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<>());
        assertNotNull(gatherVars);
        assertEquals(1, gatherVars.size());
        assertEquals("SOMEVALUE", gatherVars.get("SOMEKEY"));
    }

    @Test
    void fileWithThreeElements() throws Exception {
        checkWithThreeElements(true);
    }

    @Test
    void contentWithThreeElements() throws Exception {
        checkWithThreeElements(false);
    }

    private void checkWithThreeElements(boolean fromFile) throws Exception {
        String content = "KEY1=VALUE1\nKEY2=VALUE2\nKEY3=VALUE3";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    @Test
    void fileWithSpaceInElements() throws Exception {
        checkWithSpaceInElements(true);
    }

    @Test
    void contentWithSpaceInElements() throws Exception {
        checkWithSpaceInElements(false);
    }

    private void checkWithSpaceInElements(boolean fromFile) throws Exception {
        String content = "KEY1 =VALUE1\nKEY2=VALUE2\nKEY3=VALUE3 ";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    @Test
    void fileWithNewlineInValues() throws Exception {
        checkWithNewlineInValues(true);
    }

    @Test
    void contentWithNewlineInValues() throws Exception {
        checkWithNewlineInValues(false);
    }

    private void checkWithNewlineInValues(boolean fromFile) throws Exception {
        // Create properties file containing backslash-escaped newlines
        String content = "KEY1=line1\\nline2\nKEY2= line3 \\n\\\nline4 \nKEY3=line5\\\n\\\nline6\nKEY4=line7\\\nline8\\n\\nline9";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<>());
        assertNotNull(gatherVars);
        assertEquals(4, gatherVars.size());

        // Values should be trimmed at start & end, otherwise whitespace & newlines should be kept
        assertEquals("line1\nline2", gatherVars.get("KEY1"));
        assertEquals("line3 \nline4", gatherVars.get("KEY2"));
        assertEquals("line5line6", gatherVars.get("KEY3"));
        assertEquals("line7line8\n\nline9", gatherVars.get("KEY4"));
    }

    @Test
    void fileWithVarsToResolve() throws Exception {
        checkWithVarsToResolve(true);
    }

    @Test
    void contentWithVarsToResolve() throws Exception {
        checkWithVarsToResolve(false);
    }

    private void checkWithVarsToResolve(boolean fromFile) throws Exception {
        String content = "KEY1 =${VAR1_TO_RESOLVE}\nKEY2=https\\://github.com\nKEY3=${VAR3_TO_RESOLVE}\\\\otherContent";
        Map<String, String> currentEnvVars = new HashMap<>();
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
    void fileWithBackSlashes() throws Exception {
        checkWithBackSlashes(true);
    }

    @Test
    void contentWithBackSlashes() throws Exception {
        checkWithBackSlashes(false);
    }

    private void checkWithBackSlashes(boolean fromFile) throws Exception {
        String content = "KEY1=Test\\Path\\Variable\nKEY2=C:\\Windows\\Temp\nKEY3=\\\\Test\\Path\\Variable";
        Map<String, String> gatherVars = gatherEnvVars(fromFile, content, new HashMap<>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());

        assertEquals("Test\\Path\\Variable", gatherVars.get("KEY1"));
        assertEquals("C:\\Windows\\Temp", gatherVars.get("KEY2"));
        assertEquals("\\\\Test\\Path\\Variable", gatherVars.get("KEY3"));
    }

    private Map<String, String> gatherEnvVars(boolean fromFile, String content2Load, Map<String, String> currentEnvVars) throws Exception {
        File propFile = File.createTempFile("test", "test");
        FileUtils.writeStringToFile(propFile, content2Load, StandardCharsets.UTF_8);
        if (fromFile) {
            return propertiesLoader.getVarsFromPropertiesFile(propFile, currentEnvVars);
        } else {
            return propertiesLoader.getVarsFromPropertiesContent(content2Load, currentEnvVars);
        }
    }

}
