package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.service.PropertiesGetter;
import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectInfo implements Serializable {

    protected String propertiesFilePath;
    protected String propertiesContent;

    @Deprecated
    private transient Map<String, String> propertiesContentMap;

    @Deprecated
    protected transient boolean populateTriggerCause;

    @DataBoundConstructor
    public EnvInjectInfo(String propertiesFilePath, String propertiesContent) {
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
        this.propertiesContent = Util.fixEmpty(propertiesContent);
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    @SuppressWarnings({"unused", "deprecation"})
    public String getPropertiesContent() {

        if (propertiesContentMap != null && propertiesContentMap.size() != 0) {
            PropertiesGetter propertiesGetter = new PropertiesGetter();
            return propertiesGetter.getPropertiesContent(propertiesContentMap);
        }

        return propertiesContent;
    }

    @SuppressWarnings("deprecation")
    public Map<String, String> getPropertiesContentMap() {

        if (propertiesContentMap != null && propertiesContentMap.size() != 0) {
            return propertiesContentMap;
        }

        if (propertiesContent != null) {
            Map<String, String> contentMap = new HashMap<String, String>();
            PropertiesLoader loader = new PropertiesLoader();
            try {
                contentMap = loader.getVarsFromPropertiesContent(propertiesContent);
            } catch (EnvInjectException e) {
                e.printStackTrace();
            }
            return contentMap;
        }

        return null;
    }

}
