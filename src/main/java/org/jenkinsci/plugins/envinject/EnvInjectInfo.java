package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.service.PropertiesGetter;
import org.jenkinsci.plugins.envinject.service.PropertiesLoader;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectInfo implements Serializable {

    //TODO: Should be final, but binary compatibility...
    protected @CheckForNull String propertiesFilePath;
    protected @CheckForNull String propertiesContent;

    @DataBoundConstructor
    public EnvInjectInfo(String propertiesFilePath, String propertiesContent) {
        this.propertiesFilePath = Util.fixEmpty(propertiesFilePath);
        this.propertiesContent = fixCrLf(Util.fixEmpty(propertiesContent));
    }

    @CheckForNull
    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    @CheckForNull
    public String getPropertiesContent() {
        if (propertiesContentMap != null && propertiesContentMap.size() != 0) {
            PropertiesGetter propertiesGetter = new PropertiesGetter();
            return propertiesGetter.getPropertiesContentFromMapObject(propertiesContentMap);
        }

        return propertiesContent;
    }

    @CheckForNull
    public Map<String, String> getPropertiesContentMap(@NonNull Map<String, String> currentEnvVars) {
        if (propertiesContentMap != null && propertiesContentMap.size() != 0) {
            return propertiesContentMap;
        }

        if (propertiesContent == null) {
            return null;
        }

        if (propertiesContent.trim().length() == 0) {
            return null;
        }

        Map<String, String> contentMap = new HashMap<String, String>();
        PropertiesLoader loader = new PropertiesLoader();
        try {
            contentMap = loader.getVarsFromPropertiesContent(propertiesContent, currentEnvVars);
        } catch (EnvInjectException e) {
            e.printStackTrace();
        }
        return contentMap;
    }

    /**
     * Fix CR/LF and always make it Unix style.
     * @return String with fixed line endings. May return {@code null} only for {@code null} input
     */
    @Nullable
    protected String fixCrLf(@CheckForNull String s) {
        if (s == null) {
            return null;
        }

        // eliminate CR
        int idx;
        while ((idx = s.indexOf("\r\n")) != -1)
            s = s.substring(0, idx) + s.substring(idx + 1);
        return s;
    }

    @Deprecated
    @CheckForNull
    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Deprecated")
    private transient Map<String, String> propertiesContentMap;
    @Deprecated
    @CheckForNull
    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Deprecated")
    protected transient boolean populateTriggerCause;

}
