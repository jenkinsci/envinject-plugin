package org.jenkinsci.plugins.envinject;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectException extends Exception {

    public EnvInjectException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
