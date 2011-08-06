package org.jenkinsci.plugins.envinject.service;


import hudson.model.TaskListener;
import org.jenkinsci.plugins.envinject.EnvInjectException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Boissinot
 */
public class PropertiesFileService implements Serializable {

    private TaskListener listener;

    public PropertiesFileService(TaskListener listener) {
        this.listener = listener;
    }

    /**
     * Get a map environment variables from a properties file path
     *
     * @return a environment map
     * @throws org.jenkinsci.plugins.envinject.EnvInjectException
     *
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
            listener.getLogger().print(String.format("Injecting as environment variables the properties file path '%s'", filePath));
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
     * Gte a map environment variables from the content
     *
     * @return a environment map
     * @throws EnvInjectException
     */
    public Map<String, String> getVarsFromPropertiesContent(String fileContent) throws EnvInjectException {

        if (fileContent == null) {
            throw new NullPointerException("The file content object must be set.");
        }

        Map<String, String> result = new HashMap<String, String>();

        StringReader stringReader = new StringReader(fileContent);
        Properties properties = new Properties();
        listener.getLogger().print(String.format("Injecting as environment variables the properties content \n '%s' \n", fileContent));
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
