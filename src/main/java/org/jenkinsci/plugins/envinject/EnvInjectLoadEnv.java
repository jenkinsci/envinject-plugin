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
public class EnvInjectLoadEnv implements Callable<Map<String, String>, Throwable> {

    private EnvInjectUIInfo info;

    private BuildListener buildListener;

    public EnvInjectLoadEnv(EnvInjectUIInfo info, BuildListener buildListener) {
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
    private Map<String, String> fillMapFromPropertiesFilePath(String filePath) throws IOException {
        Map<String, String> result = new HashMap<String, String>();

        File f = new File(filePath);
        if (!f.exists()) {
            return result;
        }
        Properties properties = new Properties();
        FileReader fileReader = new FileReader(f);
        properties.load(fileReader);
        fileReader.close();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result;
    }

    private Map<String, String> fillMapFromPropertiesContent(String propertiesContent) throws IOException {
        Map<String, String> result = new HashMap<String, String>();

        StringReader stringReader = new StringReader(info.getPropertiesContent());
        Properties properties = new Properties();
        properties.load(stringReader);
        stringReader.close();

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
