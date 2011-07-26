package org.jenkinsci.plugins.envinject;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectLoadPropertiesVariables implements Callable<Map<String, String>, Throwable> {

    private EnvInjectUIInfo info;

    private BuildListener buildListener;

    public EnvInjectLoadPropertiesVariables(EnvInjectUIInfo info, BuildListener buildListener) {
        this.info = info;
        this.buildListener = buildListener;
    }

    /**
     * Fill Map environment variable from a properties file path
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    private Map<String, String> fillMapFromPropertiesFilePath(String filePath) throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        File f = new File(filePath);
        if (!f.exists()) {
            return result;
        }
        Properties properties = new Properties();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(f);
            buildListener.getLogger().print(String.format("Load the properties file path '%s'", filePath));
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

    private Map<String, String> fillMapFromPropertiesContent(String propertiesContent) throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        StringReader stringReader = new StringReader(info.getPropertiesContent());
        Properties properties = new Properties();
        buildListener.getLogger().print(String.format("Load the properties file content \n '%s'", info.getPropertiesContent()));
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


    public Map<String, String> call() throws Throwable {

        Map<String, String> result = new HashMap<String, String>();

        //Process the properties file
        if (info.getPropertiesFilePath() != null) {
            result.putAll(fillMapFromPropertiesFilePath(info.getPropertiesFilePath()));
        }

        //Process the properties content
        if (info.getPropertiesContent() != null) {
            result.putAll(fillMapFromPropertiesContent(info.getPropertiesContent()));
        }

        return result;
    }
}
