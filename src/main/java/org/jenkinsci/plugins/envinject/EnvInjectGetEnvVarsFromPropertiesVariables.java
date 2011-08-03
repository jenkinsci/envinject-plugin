package org.jenkinsci.plugins.envinject;

import hudson.model.TaskListener;
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
public class EnvInjectGetEnvVarsFromPropertiesVariables implements Callable<Map<String, String>, Throwable> {

    private EnvInjectInfo info;

    private TaskListener listener;

    public EnvInjectGetEnvVarsFromPropertiesVariables(EnvInjectInfo info, TaskListener listener) {
        this.info = info;
        this.listener = listener;
    }

    public Map<String, String> call() throws Throwable {

        Map<String, String> result = new HashMap<String, String>();

        //Process the properties file
        if (info.getPropertiesFilePath() != null) {
            result.putAll(fillMapFromAPropertiesFilePath());
        }

        //Process the properties content
        if (info.getPropertiesContent() != null) {
            result.putAll(fillMapFromPropertiesContent());
        }

        return result;
    }

    /**
     * Fill a map environment variables from a properties file path
     *
     * @return a environment map
     * @throws EnvInjectException
     */
    private Map<String, String> fillMapFromAPropertiesFilePath() throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        //The path is relative from root execution or it's a relative path
        File f = new File(info.getPropertiesFilePath());
        if (!f.exists()) {
            return result;
        }
        Properties properties = new Properties();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(f);
            listener.getLogger().print(String.format("Injecting as environment variables the properties file path '%s'", info.getPropertiesFilePath()));
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
     * Fill a map environment variables from the content
     *
     * @return a environment map
     * @throws EnvInjectException
     */
    private Map<String, String> fillMapFromPropertiesContent() throws EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        StringReader stringReader = new StringReader(info.getPropertiesContent());
        Properties properties = new Properties();
        listener.getLogger().print(String.format("Injecting as environment variables the properties content \n '%s' \n", info.getPropertiesContent()));
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
