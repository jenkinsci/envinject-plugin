package org.jenkinsci.plugins.envinject;

import hudson.model.Api;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@ExportedBean
public class EnvInjectVarList implements Serializable {

    private Map<String, String> envMap = new HashMap<String, String>();

    public EnvInjectVarList(Map<String, String> envMap) {
        this.envMap = envMap;
    }

    @Exported
    @SuppressWarnings("unused")
    public Map<String, String> getEnvMap() {
        return envMap;
    }

    @SuppressWarnings("unused")
    public Api getApi() {
        return new Api(this);
    }

}
