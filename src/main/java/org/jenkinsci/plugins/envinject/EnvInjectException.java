package org.jenkinsci.plugins.envinject;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class EnvInjectException extends Exception {

    public EnvInjectException(String s) {
        super(s);
    }

    public EnvInjectException(Throwable throwable) {
        super(throwable);
    }

    public EnvInjectException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
