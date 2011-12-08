package org.jenkinsci.plugins.envinject;

import hudson.model.TaskListener;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectLogger implements Serializable {

    private TaskListener listener;

    public EnvInjectLogger(TaskListener listener) {
        this.listener = listener;
    }

    public TaskListener getListener() {
        return listener;
    }

    public void info(String message) {
        listener.getLogger().println("[EnvInject] - " + message);
    }

    public void error(String message) {
        listener.getLogger().println("[EnvInject] - [ERROR] - " + message);
    }

}
