package org.jenkinsci.plugins.envinject;

import hudson.Util;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.service.PropertiesGetter;
import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectInfo implements Serializable {

    protected String propertiesFilePath;

    private Map<String, String> propertiesContentMap;

    protected transient String propertiesContent;
    protected transient boolean populateTriggerCause;

    @DataBoundConstructor
    public EnvInjectInfo(String propertiesFilePath, String propertiesContent) {
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
        PropertiesLoader loader = new PropertiesLoader();
        try {
            propertiesContentMap = loader.getVarsFromPropertiesContent(propertiesContent);
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }
    }

    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public Map<String, String> getPropertiesContentMap() {
        return propertiesContentMap;
    }

    @SuppressWarnings("unused")
    public String getPropertiesContent() {
        if (propertiesContentMap != null) {
            PropertiesGetter propertiesGetter = new PropertiesGetter();
            return propertiesGetter.getPropertiesContent(propertiesContentMap);
        }
        return null;
    }

    private Object readResolve() throws IOException {
        if (propertiesContent != null) {
            PropertiesLoader loader = new PropertiesLoader();
            try {
                propertiesContentMap = loader.getVarsFromPropertiesContent(propertiesContent);
            } catch (EnvInjectException e) {
                e.printStackTrace();
            }
        }
        return this;
    }
}
