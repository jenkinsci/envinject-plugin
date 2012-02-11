package org.jenkinsci.plugins.envinject.service;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesGetter implements Serializable {

    public String getPropertiesContent(Map<String, String> propertiesContent) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : propertiesContent.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }
}
