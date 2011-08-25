package org.jenkinsci.plugins.envinject.service;


import org.jenkinsci.plugins.envinject.EnvInjectException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Boissinot
 */
public class PropertiesFileService implements Serializable {

    /**
     * Get a map environment variables from a properties file path
     *
     * @param filePath
     * @return
     * @throws EnvInjectException
     */
    public Map<String, String> getVarsFromPropertiesFilePath(String filePath) throws EnvInjectException {

        if (filePath == null) {
            throw new NullPointerException("The file path object must be set.");
        }

        Map<String, String> result = new HashMap<String, String>();

        File f = new File(filePath);
        if (!f.exists()) {
            return result;
        }
        Properties properties = new Properties();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(f);
            properties.load(fileReader);
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    throw new EnvInjectException("Problem occurs on loading content", e);
                }
            }
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result;
    }

    /**
     * Get a map environment variables from the content
     *
     * @param fileContent
     * @return
     * @throws EnvInjectException
     */
    public Map<String, String> getVarsFromPropertiesContent(String fileContent) throws EnvInjectException {

        if (fileContent == null) {
            throw new NullPointerException("The file content object must be set.");
        }

        Map<String, String> result = new HashMap<String, String>();

        StringReader stringReader = new StringReader(fileContent);
        Properties properties = new Properties();
        try {
            properties.load(stringReader);
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        } finally {
            stringReader.close();
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result;
    }


}
