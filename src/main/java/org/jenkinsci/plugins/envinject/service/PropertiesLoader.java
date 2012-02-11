package org.jenkinsci.plugins.envinject.service;


import hudson.Util;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.util.SortedProperties;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesLoader implements Serializable {

    /**
     * Get a map environment variables from a properties file path
     *
     * @param propertiesFile the properties file
     * @return a map containing all the file properties content
     * @throws EnvInjectException
     */
    public Map<String, String> getVarsFromPropertiesFile(File propertiesFile, Map<String, String> currentEnvVars) throws EnvInjectException {

        if (propertiesFile == null) {
            throw new NullPointerException("The properties file object must be set.");
        }
        if (!propertiesFile.exists()) {
            throw new NullPointerException("The properties file object must be exist.");
        }

        Map<String, String> result = new LinkedHashMap<String, String>();

        SortedProperties properties = new SortedProperties();
        try {
            String fileContent = Util.loadFile(propertiesFile);
            fileContent = processWindowsFilePath(fileContent);
            String fileContentResolved = Util.replaceMacro(fileContent, currentEnvVars);
            properties.load(new StringReader(fileContentResolved));
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(processProperty(entry.getKey()), processProperty(entry.getValue()));
        }
        return result;
    }

    /**
     * Get a map environment variables from the content
     *
     * @param content
     * @return
     * @throws EnvInjectException
     */
    public Map<String, String> getVarsFromPropertiesContent(String content) throws EnvInjectException {

        if (content == null) {
            throw new NullPointerException("The file content object must be set.");
        }

        content = processWindowsFilePath(content);

        Map<String, String> result = new LinkedHashMap<String, String>();
        StringReader stringReader = new StringReader(content);
        SortedProperties properties = new SortedProperties();
        try {
            properties.load(stringReader);
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        } finally {
            stringReader.close();
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(processProperty(entry.getKey()), processProperty(entry.getValue()));
        }
        return result;
    }

    private String processProperty(Object prop) {
        if (prop == null) {
            return null;
        }
        return String.valueOf(prop).trim();
    }

    private String processWindowsFilePath(String content) {
        if (content == null) {
            return null;
        }
        content = content.replace("\\\\", "\\");
        return content.replace("\\", "\\\\");
    }

}

