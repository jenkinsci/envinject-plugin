package org.jenkinsci.plugins.envinject.service;

import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever extends MasterToSlaveFileCallable<Map<String, String>> {

    @CheckForNull
    private String propertiesFilePath;

    @CheckForNull
    private Map<String, String> propertiesContent;

    @NonNull
    private Map<String, String> currentEnvVars;

    @NonNull
    private EnvInjectLogger logger;

    public PropertiesVariablesRetriever(
            @CheckForNull String propertiesFilePath, 
            @CheckForNull Map<String, String> propertiesContent, 
            @NonNull Map<String, String> currentEnvVars, 
            @NonNull EnvInjectLogger logger) {
        this.propertiesFilePath = propertiesFilePath;
        this.propertiesContent = propertiesContent;
        this.currentEnvVars = currentEnvVars;
        this.logger = logger;
    }

    //TODO: Actually Channel is not used here. Maybe a bug?
    @Override
    public Map<String, String> invoke(@CheckForNull File base, @CheckForNull VirtualChannel channel) throws IOException, InterruptedException {
        Map<String, String> result = new LinkedHashMap<String, String>();

        try {

            PropertiesLoader loader = new PropertiesLoader();

            //Add the properties file
            if (propertiesFilePath != null) {
                String propertiesFilePathResolved = Util.replaceMacro(propertiesFilePath, currentEnvVars);
                propertiesFilePathResolved = propertiesFilePathResolved.replace("\\", "/");
                File propertiesFile = getFile(base, propertiesFilePathResolved);
                if (propertiesFile == null) {
                    String message = String.format("The given properties file path '%s' doesn't exist.", propertiesFilePathResolved);
                    logger.error(message);
                    String patternMessage = String.format("Missing file path was resolved from pattern '%s' .", propertiesFilePath);
                    logger.error(patternMessage);
                    throw new EnvInjectException(message);
                }
                logger.info(String.format("Injecting as environment variables the properties file path '%s'", propertiesFilePathResolved));
                result.putAll(loader.getVarsFromPropertiesFile(propertiesFile, currentEnvVars));
                logger.info("Variables injected successfully.");
            }

            //Add the properties content
            if (propertiesContent != null) {
                PropertiesGetter propertiesGetter = new PropertiesGetter();
                logger.info(String.format("Injecting as environment variables the properties content %n%s%n", propertiesGetter.getPropertiesContentFromMapObject(propertiesContent)));
                for (Map.Entry<String, String> entry : propertiesContent.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (result.containsKey(key)) {
                        // a variable is defined in both propertiesFilePath and propertiesContent
                        // instead of just overwriting it we possibly resolve it against the old value
                        Map<String, String> oldValueMap = new LinkedHashMap<String, String>();
                        oldValueMap.put(key, result.get(key));
                        value = Util.replaceMacro(value, oldValueMap);
                    }
                    result.put(key, value);
                }
                logger.info("Variables injected successfully.");
            }

        } catch (EnvInjectException envEx) {
            throw new IOException(envEx.getMessage());
        }

        return result;
    }

    @CheckForNull
    private File getFile(@CheckForNull File base, @NonNull String scriptFilePath) {

        File file = new File(scriptFilePath);
        if (file.exists()) {
            return file;
        }

        file = new File(base, scriptFilePath);
        return file.exists() ? file : null;
    }

}
