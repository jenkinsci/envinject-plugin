package org.jenkinsci.plugins.envinject;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordEntry implements Serializable {

    @CheckForNull 
    private String name;

    @NonNull
    private final Secret value;

    @DataBoundConstructor
    public EnvInjectPasswordEntry(@CheckForNull String name, @CheckForNull String password) {
        //TODO: Fix empty and trim?
        this.name = name;
        this.value = Secret.fromString(password);
    }

    @CheckForNull 
    public String getName() {
        return name;
    }

    @NonNull 
    public Secret getValue() {
        return value;
    }
}
