package org.jenkinsci.plugins.envinject;

import hudson.model.Cause;

/**
 * @author Gregory Boissinot
 */
public class CustomTestCause extends Cause {

    @Override
    public String getShortDescription() {
        return "Short Custom Cause";
    }

}
