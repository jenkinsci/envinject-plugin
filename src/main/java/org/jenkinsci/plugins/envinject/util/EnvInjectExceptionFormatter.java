package org.jenkinsci.plugins.envinject.util;

import org.jenkinsci.lib.envinject.EnvInjectException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Formats {@link EnvInjectException}s.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class EnvInjectExceptionFormatter {
    
    private EnvInjectExceptionFormatter() {
        // Cannot be constructed
    }
    
    public static EnvInjectException forProhibitedLoadFromMaster(String path) {
        return new EnvInjectException("Cannot load file " + path + " from the master. "
            + "Loading of files from the master is prohibited globally.");
    }
    
}
