package org.jenkinsci.plugins.envinject.util;

import hudson.model.Environment;
import hudson.model.Run;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment, which does not override already injected variables.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class NonOverridingEnvironment extends Environment {

    //TODO: Consider using RunListener to report collisions, causes too much buildlog spam now
    private static final Logger LOGGER = Logger.getLogger(NonOverridingEnvironment.class.getName());

    private final Run<?, ?> run;

    @CheckForNull
    private final Map<String, String> vars;

    public NonOverridingEnvironment(@Nonnull Run<?, ?> run, @CheckForNull Map<String, String> vars) {
        this.vars = vars;
        this.run = run;
    }

    @Override
    public void buildEnvVars(Map<String, String> env) {
        append(run, env, vars);
    }

    public static void append(@Nonnull Run<?, ?> run, @Nonnull Map<String, String> env, @CheckForNull Map<String, String> toInject) {
        if (toInject == null) {
            return; // nothing to inject
        }

        for (Map.Entry<String, String> storedVar : toInject.entrySet()) {
            final String varName = storedVar.getKey();
            final String storedValue = storedVar.getValue();
            final String envValue = env.get(storedVar.getKey());
            if (envValue == null) {
                LOGGER.log(Level.CONFIG, "Run {0}: Variable {1} is missing, injecting {2}",
                        new Object[] {run, varName, storedValue});
                env.put(varName, storedValue);
            } else if (LOGGER.isLoggable(Level.FINER) && !envValue.equals(storedValue)) {
                LOGGER.log(Level.FINER, "Run {0}: Variable {1} is already defined, keeping {2} instead of {3}",
                        new Object[] {run, varName, storedValue, envValue});
            }
        }
    }
}
