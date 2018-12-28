package org.jenkinsci.plugins.envinject.service;

import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.PasswordParameterValue;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectGlobalPasswordEntry;
import org.jenkinsci.plugins.envinject.EnvInjectPasswordEntry;
import org.jenkinsci.plugins.envinject.EnvInjectPasswordWrapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Masks {@link PasswordParameterValue}s
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordsMasker implements Serializable {

    /**
     *
     * @param run Run
     * @param logger Logger
     * @param envVars Target collection with Environment variables to be masked
     *
     * @deprecated Use {@link #maskPasswordParametersIfAny(hudson.model.Run, java.util.Map, org.jenkinsci.lib.envinject.EnvInjectLogger)}
     */
    @Deprecated
    public void maskPasswordsIfAny(@Nonnull AbstractBuild run, @Nonnull EnvInjectLogger logger, @Nonnull Map<String, String> envVars) {
        maskPasswordParametersIfAny(run, envVars, logger);
    }
    
    /**
     * Masks {@link PasswordParameterValue}s.
     * @param run Run
     * @param envVars Target collection with Environment variables to be masked
     * @param logger Logger
     * @since 2.1
     */
    public void maskPasswordParametersIfAny(@Nonnull Run<?, ?> run, @Nonnull Map<String, String> envVars, @Nonnull EnvInjectLogger logger) {
        maskPasswordsJobParameterIfAny(run, logger, envVars);
        maskPasswordsEnvInjectIfAny(run, logger, envVars);
    }

    private void maskPasswordsJobParameterIfAny(@Nonnull Run<?, ?> run, 
            @Nonnull EnvInjectLogger logger, @Nonnull Map<String, String> envVarsTarget) {
        ParametersAction parametersAction = run.getAction(ParametersAction.class);
        if (parametersAction != null) {
            List<ParameterValue> parameters = parametersAction.getParameters();
            if (parameters != null) {
                for (ParameterValue parameter : parameters) {
                    if (parameter instanceof PasswordParameterValue) {
                        PasswordParameterValue passwordParameterValue = ((PasswordParameterValue) parameter);
                        envVarsTarget.put(passwordParameterValue.getName(), passwordParameterValue.getValue().getEncryptedValue());
                    }
                }
            }
        }
    }

    private void maskPasswordsEnvInjectIfAny(@Nonnull Run<?, ?> build, 
            @Nonnull EnvInjectLogger logger, @Nonnull Map<String, String> envVars) {
        try {

            EnvInjectPasswordWrapper envInjectPasswordWrapper = getEnvInjectPasswordWrapper(build);
            if (envInjectPasswordWrapper == null) {
                //nothing to mask
                return;
            }

            if (envInjectPasswordWrapper.isInjectGlobalPasswords()) {
                //Global passwords
                maskGlobalPasswords(envVars);
            }

            //Job passwords
            maskJobPasswords(envVars, envInjectPasswordWrapper.getPasswordEntryList());

        } catch (EnvInjectException ee) {
            logger.error("Can't mask global password :" + ee.getMessage());
        }
    }

    @CheckForNull
    private EnvInjectPasswordWrapper getEnvInjectPasswordWrapper(@Nonnull Run<?, ?> build) throws EnvInjectException {

        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject;
        if (build instanceof MatrixRun) {
            MatrixProject project = ((MatrixRun) build).getParentBuild().getProject();
            wrappersProject = project.getBuildWrappersList();
        } else {
            final Job<?, ?> job = build.getParent();
            if (job instanceof BuildableItemWithBuildWrappers) {
                BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers) job;
                wrappersProject = project.getBuildWrappersList();
            } else {
                throw new EnvInjectException(String.format("Job type %s is not supported", job));
            }
        }

        for (BuildWrapper buildWrapper : wrappersProject) {
            if (EnvInjectPasswordWrapper.class.equals(buildWrapper.getClass())) {
                return (EnvInjectPasswordWrapper) buildWrapper;
            }
        }

        return null;
    }

    private void maskGlobalPasswords(Map<String, String> envVarsTarget) throws EnvInjectException {
        EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
        EnvInjectGlobalPasswordEntry[] globalPasswordEntries = globalPasswordRetriever.getGlobalPasswords();
        if (globalPasswordEntries != null) {
            for (EnvInjectGlobalPasswordEntry globalPasswordEntry : globalPasswordEntries) {
                envVarsTarget.put(globalPasswordEntry.getName(),
                        globalPasswordEntry.getValue().getEncryptedValue());
            }
        }
    }

    private void maskJobPasswords(@Nonnull Map<String, String> envVarsTarget, @Nonnull List<EnvInjectPasswordEntry> passwordEntries) {
        for (EnvInjectPasswordEntry passwordEntry : passwordEntries) {
            envVarsTarget.put(passwordEntry.getName(), passwordEntry.getValue().getEncryptedValue());
        }
    }


}
