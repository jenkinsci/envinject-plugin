package org.jenkinsci.plugins.envinject.service;

import java.io.Serializable;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * @author Gregory Boissinot
 */
public class PropertiesGetter implements Serializable {

    @Nullable
    public String getPropertiesContentFromMapObject(@CheckForNull Map<String, String> propertiesContent) {

        if (propertiesContent == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : propertiesContent.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }
}
