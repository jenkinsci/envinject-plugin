package org.jenkinsci.plugins.envinject;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectGlobalPasswordEntry extends EnvInjectPasswordEntry {

    @DataBoundConstructor
    public EnvInjectGlobalPasswordEntry(String name, String password) {
        super(name, password);
    }
}