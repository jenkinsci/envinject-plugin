package org.jenkinsci.plugins.envinject.service;

import org.jenkinsci.plugins.envinject.EnvInjectLogger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectEnvVarsUnset implements Serializable {

    EnvInjectLogger logger;

    public EnvInjectEnvVarsUnset(EnvInjectLogger logger) {
        this.logger = logger;
    }

    public Map<String, String> removeUnsetVars(Map<String, String> envVars) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            if (!isUnresolvedVar(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                logger.info(String.format("Unset '%s' variable.", entry.getKey()));
            }
        }
        return result;
    }

    private boolean isUnresolvedVar(String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith("$");
    }
}
