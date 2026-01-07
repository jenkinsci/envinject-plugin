package org.jenkinsci.plugins.envinject.service;

import hudson.EnvVars;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.envinject.EnvInjectGlobalStorage;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.util.Map;

@Restricted(NoExternalUse.class)
public class EnvInjectMasterEnvVarsRetriever extends MasterToSlaveCallable<Map<String, String>, IOException> {

    public Map<String, String> call() throws IOException {
        return EnvInjectGlobalStorage.getMergedVars(EnvVars.masterEnvVars);
    }
}
