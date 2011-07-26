package org.jenkinsci.plugins.envinject;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectException extends Exception {

    public EnvInjectException() {
    }

    public EnvInjectException(String s) {
        super(s);
    }

    public EnvInjectException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public EnvInjectException(Throwable throwable) {
        super(throwable);
    }
}
