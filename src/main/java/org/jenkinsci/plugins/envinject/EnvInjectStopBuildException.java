package org.jenkinsci.plugins.envinject;

import hudson.model.Result;
import org.jenkinsci.lib.envinject.EnvInjectException;

import javax.annotation.Nonnull;

/**
 * Signals the plugin to stop the build and set the build result as specified,
 * as opposed to Failure in the case of a RuntimeException
 */
public class EnvInjectStopBuildException extends EnvInjectException {

    private final Result result;

    public EnvInjectStopBuildException(@Nonnull String s, @Nonnull Result result) {
        super(s);
        this.result = result;
    }

    public Result getResult() {
        return result;
    }
}
