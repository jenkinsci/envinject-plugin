package org.jenkinsci.plugins.envinject.service;

import hudson.Util;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class PropertiesLoader implements Serializable {

    /**
     * Get environment variables from a properties file path
     *
     * @param propertiesFile the properties file
     * @param currentEnvVars the current environment variables to resolve against
     * @return the environment variables
     * @throws EnvInjectException Issue with content loading or processing
     */
    @NonNull
    public Map<String, String> getVarsFromPropertiesFile(@NonNull File propertiesFile, @NonNull Map<String, String> currentEnvVars) 
            throws EnvInjectException {
        if (propertiesFile == null) {
            throw new NullPointerException("The properties file object must be set.");
        }
        if (!propertiesFile.exists()) {
            throw new IllegalArgumentException("The properties file object must be exist.");
        }

        try {
            String fileContent = Util.loadFile(propertiesFile);
            return getVars(fileContent, currentEnvVars);
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        }
    }

    /**
     * Get a map environment variables from the content
     *
     * @param content        the properties content to parse
     * @param currentEnvVars the current environment variables to resolve against
     * @return the environment variables
     * @throws EnvInjectException Issue with content loading or processing
     */
    @NonNull
    public Map<String, String> getVarsFromPropertiesContent(@NonNull String content, @NonNull Map<String, String> currentEnvVars) throws EnvInjectException {
        if (content == null) {
            throw new NullPointerException("A properties content must be set.");
        }
        if (content.trim().length() == 0) {
            throw new IllegalArgumentException("A properties content must be not empty.");
        }

        return getVars(content, currentEnvVars);
    }

    @NonNull
    private Map<String, String> getVars(@NonNull String content, @NonNull Map<String, String> currentEnvVars) 
            throws EnvInjectException {

        String[] contentArray = content.split("\n");
        content = "";
        //Check if string contains windows network or local paths
        for (String s : contentArray) {
            if (s.indexOf("=\\\\", 0) != -1 || Pattern.matches("[A-z_0-9]+=[A-z]:\\\\.*", s)) {
                // Replace single backslashes with double ones so they won't be removed by Property.load()
                s = s.replaceAll("(?<![\\\\])\\\\(?![:*?\"<>\\\\/])(?![\\\\])(?![\n])", "\\\\\\\\");
                //Escape windows network shares initial double backslash i.e \\Network\Share
                s = s.replaceAll("(?m)^([^=]+=)(\\\\\\\\)(?![:*?\"<>\\\\/])", "$1\\\\\\\\\\\\\\\\");
            }
            content += s + "\n";
        }
        Map<String, String> result = new LinkedHashMap<>();
        
        Properties properties = new Properties();

        try (StringReader stringReader = new StringReader(content)) {
            properties.load(stringReader);
        } catch (IOException ioe) {
            throw new EnvInjectException("Problem occurs on loading content", ioe);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(processElement(entry.getKey(), currentEnvVars), processElement(entry.getValue(), currentEnvVars));
        }
        return result;
    }

    @CheckForNull
    private String processElement(@CheckForNull Object prop, @NonNull Map<String, String> currentEnvVars) {
        String macroProcessedElement = Util.replaceMacro(String.valueOf(prop), currentEnvVars);
        if (macroProcessedElement == null) {
            return null;
        }
        return macroProcessedElement.trim();
    }
}

