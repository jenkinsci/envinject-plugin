package org.jenkinsci.plugins.envinject;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Items;

/**
 * @author Gregory Boissinot
 */
public class AliasInitializer {

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    @SuppressWarnings("unused")
    public static void addAliases() {
        Items.XSTREAM.alias("EnvInjectJobProperty", EnvInjectJobProperty.class);
        Items.XSTREAM.alias("EnvInjectBuildWrapper", EnvInjectBuildWrapper.class);
        Items.XSTREAM.alias("EnvInjectPasswordWrapper", EnvInjectPasswordWrapper.class);
        Items.XSTREAM.alias("EnvInjectPasswordEntry", EnvInjectPasswordEntry.class);
        Items.XSTREAM.alias("EnvInjectBuilder", EnvInjectBuilder.class);
    }
}
