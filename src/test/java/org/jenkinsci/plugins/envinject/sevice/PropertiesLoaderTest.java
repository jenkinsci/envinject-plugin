package org.jenkinsci.plugins.envinject.sevice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.junit.Test;

/**
 * @author Gregory Boissinot
 */
public class PropertiesLoaderTest {

    PropertiesLoader propertiesLoader = new PropertiesLoader();

    
    //-- File

    /**
     * Test null properties File.
     * @throws Exception for null File.
     */
    @Test(expected = NullPointerException.class)
    public void nullFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(null, new HashMap<String, String>());
    }

    /**
     * Test new file with non-existing path.
     * @throws Exception for non-existing path.
     */
    @Test(expected = IllegalArgumentException.class)
    public void notExistFile() throws Exception {
        propertiesLoader.getVarsFromPropertiesFile(new File("not exist"), new HashMap<String, String>());
    }

    /**
     * Test file with no content.
     * @throws Exception on file creation.
     */
    @Test
    public void emptyFile() throws Exception {
        File emptyFile = File.createTempFile("test", "test");
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(emptyFile, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(0, gatherVars.size());
    }

    //-- Content

    /**
     * Test null content in the interface field.
     * @throws Exception for null content.
     */
    @Test(expected = NullPointerException.class)
    public void nullContent() throws Exception {
        propertiesLoader.getVarsFromPropertiesContent(null, new HashMap<String, String>());
    }

    /**
     * Test empty content in the interface field.
     * @throws Exception for invalid content.
     */
    @Test(expected = IllegalArgumentException.class)
    public void emptyContent() throws Exception {
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(new String(), currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(0, gatherVars.size());
    }

    //--Both

    /**
     * Test properties file with a single key/value pair.
     * @throws Exception on file read/write
     */
    @Test
    public void fileWithOneElement() throws Exception {
	// Create properties file containing backslash-escaped newlines
	Properties prop = new Properties();
	prop.setProperty("SOMEKEY", "SOMEVALUE");

	File propFile = File.createTempFile("test", "test");
	OutputStream output = new FileOutputStream(propFile);
	prop.store(output, null);
	
	Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(propFile, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(1, gatherVars.size());
        assertEquals("SOMEVALUE", gatherVars.get("SOMEKEY"));
    }

    /**
     * Test content with a single key/value pair.
     * @throws Exception reading content.
     */
    @Test
    public void contentWithOneElement() throws Exception {
	String content = "SOMEKEY=SOMEVALUE";
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(1, gatherVars.size());
        assertEquals("SOMEVALUE", gatherVars.get("SOMEKEY"));
    }

    /**
     * Test properties file with three key/value pairs.
     * @throws Exception on file read/write
     */
    @Test
    public void fileWithThreeElements() throws Exception {
	// Create properties file containing backslash-escaped newlines
	String keys[] = { "VALUE1", "VALUE2", "VALUE3" };

	Properties prop = new Properties();
	prop.setProperty("KEY1", keys[0]);
	prop.setProperty("KEY2", keys[1]);
	prop.setProperty("KEY3", keys[2]);

	File propFile = File.createTempFile("test", "test");
	OutputStream output = new FileOutputStream(propFile);
	prop.store(output, null);

	// Read properties file using envInject file loader
	Map<String, String> gatherVars = propertiesLoader
		.getVarsFromPropertiesFile(propFile,
			new HashMap<String, String>());
	
	assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals(keys[0], gatherVars.get("KEY1"));
        assertEquals(keys[1], gatherVars.get("KEY2"));
        assertEquals(keys[2], gatherVars.get("KEY3"));
    }

    /**
     * Test content with three key/value pairs.
     * @throws Exception reading content.
     */
    @Test
    public void contentWithThreeElements() throws Exception {
	String content = "KEY1=VALUE1\nKEY2=VALUE2\nKEY3=VALUE3";
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    /**
     * Test properties file where keys and/or values have spaces.
     * @throws Exception on file read/write
     */
    @Test
    public void fileWithSpaceInElements() throws Exception {
	// Create properties file containing backslash-escaped newlines
	String keys[] = { "VALUE1", "VALUE2", "VALUE3 ", " VALUE4 " };

	Properties prop = new Properties();
	prop.setProperty("KEY1 ", keys[0]);
	prop.setProperty("KEY2", keys[1]);
	prop.setProperty("KEY3", keys[2]);
	prop.setProperty("KEY4", keys[3]);

	File propFile = File.createTempFile("test", "test");
	OutputStream output = new FileOutputStream(propFile);
	prop.store(output, null);

	// Read properties file using envInject file loader
	Map<String, String> gatherVars = propertiesLoader
		.getVarsFromPropertiesFile(propFile,
			new HashMap<String, String>());

	assertNotNull(gatherVars);
	assertEquals(3, gatherVars.size());
	assertEquals(keys[0], gatherVars.get("KEY1"));
	assertEquals(keys[1], gatherVars.get("KEY2"));
	assertEquals(keys[2], gatherVars.get("KEY3"));
	assertEquals(keys[3], gatherVars.get("KEY4"));
    }

    /**
     * Test content where keys and/or values have spaces.
     * @throws Exception reading content.
     */
    @Test
    public void contentWithSpaceInElements() throws Exception {
	String content = "KEY1 =VALUE1\nKEY2=VALUE2\nKEY3=VALUE3 ";
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());
        assertEquals("VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("VALUE3", gatherVars.get("KEY3"));
    }

    /**
     * Test properties file with newlines in the values.
     * @throws Exception on file read/write
     */
    @Test
    public void fileWithNewlineInValues() throws Exception {
	// Create properties file containing backslash-escaped newlines
	String keys[] = { "line1\nline2", "line1 \nline2", "line1\n\nline3" };

	Properties prop = new Properties();
	prop.setProperty("KEY1", keys[0]);
	prop.setProperty("KEY2", keys[1]);
	prop.setProperty("KEY3", keys[2]);

	File propFile = File.createTempFile("test", "test");
	OutputStream output = new FileOutputStream(propFile);
	prop.store(output, null);

	// Read properties file using envInject file loader
	Map<String, String> gatherVars = propertiesLoader
		.getVarsFromPropertiesFile(propFile,
			new HashMap<String, String>());
	assertNotNull(gatherVars);
	assertEquals(3, gatherVars.size());

	// Values should be trimmed at start & end, otherwise whitespace &
	// newlines should be kept
	assertEquals(keys[0], gatherVars.get("KEY1"));
	assertEquals(keys[1], gatherVars.get("KEY2"));
	assertEquals(keys[2], gatherVars.get("KEY3"));
    }

    /**
     * Test content with newlines in the values.
     * @throws Exception reading content.
     */
    @Test
    public void contentWithNewlineInValues() throws Exception {
	// Create properties file containing backslash-escaped newlines
        String content = "KEY1=line1\\\nline2\nKEY2= line1 \\\n line2 \nKEY3=line1\\\n\\\nline3";
        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(content, new HashMap<String, String>());
        assertNotNull(gatherVars);
        assertEquals(3, gatherVars.size());

        // Values should be trimmed at start & end, otherwise whitespace & newlines should be kept
        assertEquals("line1\nline2", gatherVars.get("KEY1"));
        assertEquals("line1 \nline2", gatherVars.get("KEY2"));
        assertEquals("line1\n\nline3", gatherVars.get("KEY3"));
    }

    /**
     * Test properties file where keys and/or values are environment variables.
     * @throws Exception on file read/write.
     */
    @Test
    public void fileWithVarsToResolve() throws Exception {
        // Preset environment variables
	Map<String, String> currentEnvVars = new HashMap<String, String>();
        currentEnvVars.put("VAR1_TO_RESOLVE", "NEW_VALUE1");
        currentEnvVars.put("VAR3_TO_RESOLVE", "NEW_VALUE3");
        currentEnvVars.put("KEY4_TO_RESOLVE", "NEW_KEY4");
        
	// Create properties file containing backslash-escaped newlines
	Properties prop = new Properties();
	prop.setProperty("KEY1", "${VAR1_TO_RESOLVE}");
	prop.setProperty("KEY2", "VALUE2");
	prop.setProperty("KEY3", "${VAR3_TO_RESOLVE}\\otherContent");
	prop.setProperty("${KEY4_TO_RESOLVE}", "VALUE4");

	File propFile = File.createTempFile("test", "test");
	OutputStream output = new FileOutputStream(propFile);
	prop.store(output, null);
	
	Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesFile(propFile, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(4, gatherVars.size());
        assertEquals("NEW_VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("NEW_VALUE3\\otherContent", gatherVars.get("KEY3"));
        assertEquals("VALUE4", gatherVars.get("NEW_KEY4"));
    }

    /**
     * Test content where keys and/or values are environment variables.
     * @throws Exception reading content.
     */
    @Test
    public void contentWithVarsToResolve() throws Exception {
	String content = "KEY1 =${VAR1_TO_RESOLVE}\nKEY2=VALUE2\nKEY3=${VAR3_TO_RESOLVE}\\otherContent\n${KEY4_TO_RESOLVE}=VALUE4";
        Map<String, String> currentEnvVars = new HashMap<String, String>();
        currentEnvVars.put("VAR1_TO_RESOLVE", "NEW_VALUE1");
        currentEnvVars.put("VAR3_TO_RESOLVE", "NEW_VALUE3");
        currentEnvVars.put("KEY4_TO_RESOLVE", "NEW_KEY4");

        Map<String, String> gatherVars = propertiesLoader.getVarsFromPropertiesContent(content, currentEnvVars);
        assertNotNull(gatherVars);
        assertEquals(4, gatherVars.size());
        assertEquals("NEW_VALUE1", gatherVars.get("KEY1"));
        assertEquals("VALUE2", gatherVars.get("KEY2"));
        assertEquals("NEW_VALUE3\\otherContent", gatherVars.get("KEY3"));
        assertEquals("VALUE4", gatherVars.get("NEW_KEY4"));
    }

}
