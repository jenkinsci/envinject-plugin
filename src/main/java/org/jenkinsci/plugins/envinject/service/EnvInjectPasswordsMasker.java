package org.jenkinsci.plugins.envinject.service;

import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
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

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordsMasker implements Serializable {


    public void maskPasswordsIfAny(AbstractBuild build, EnvInjectLogger logger, Map<String, String> envVars) {
        maskPasswordsJobParameterIfAny(build, logger, envVars);
        maskPasswordsEnvInjectIfAny(build, logger, envVars);
    }

    private void maskPasswordsJobParameterIfAny(AbstractBuild build, EnvInjectLogger logger, Map<String, String> envVars) {
        ParametersAction parametersAction = build.getAction(ParametersAction.class);
        if (parametersAction != null) {
            List<ParameterValue> parameters = parametersAction.getParameters();
            if (parameters != null) {
                for (ParameterValue parameter : parameters) {
                    if (parameter instanceof PasswordParameterValue) {
                        PasswordParameterValue passwordParameterValue = ((PasswordParameterValue) parameter);
                        envVars.put(passwordParameterValue.getName(), passwordParameterValue.getValue().getEncryptedValue());
                    }
                }
            }
        }
    }

    private void maskPasswordsEnvInjectIfAny(AbstractBuild build, EnvInjectLogger logger, Map<String, String> envVars) {
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
            EnvInjectPasswordEntry[] passwordEntries = envInjectPasswordWrapper.getPasswordEntries();
            if (passwordEntries != null) {
                maskJobPasswords(envVars, passwordEntries);
            }

        } catch (EnvInjectException ee) {
            logger.error("Can't mask global password :" + ee.getMessage());
        }
    }


    private EnvInjectPasswordWrapper getEnvInjectPasswordWrapper(AbstractBuild build) throws EnvInjectException {

        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject;
        if (build instanceof MatrixRun) {
            MatrixProject project = ((MatrixRun) build).getParentBuild().getProject();
            wrappersProject = project.getBuildWrappersList();
        } else {
            AbstractProject abstractProject = build.getProject();
            if (abstractProject instanceof FreeStyleProject) {
                Project project = (Project) abstractProject;
                wrappersProject = project.getBuildWrappersList();
            } else if (abstractProject instanceof MavenModuleSet) {
                MavenModuleSet moduleSet = (MavenModuleSet) abstractProject;
                wrappersProject = moduleSet.getBuildWrappersList();
            } else if (Hudson.getInstance().getPlugin("ivy") != null && abstractProject instanceof hudson.ivy.IvyModuleSet) {
                hudson.ivy.IvyModuleSet ivyModuleSet = (hudson.ivy.IvyModuleSet) abstractProject;
                wrappersProject = ivyModuleSet.getBuildWrappersList();
            } else {
                throw new EnvInjectException(String.format("Job type %s is not supported", abstractProject));
            }
        }

        for (BuildWrapper buildWrapper : wrappersProject) {
            if (EnvInjectPasswordWrapper.class.equals(buildWrapper.getClass())) {
                return (EnvInjectPasswordWrapper) buildWrapper;
            }
        }

        return null;
    }

    private void maskGlobalPasswords(Map<String, String> envVars) throws EnvInjectException {
        EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
        EnvInjectGlobalPasswordEntry[] globalPasswordEntries = globalPasswordRetriever.getGlobalPasswords();
        if (globalPasswordEntries != null) {
            for (EnvInjectGlobalPasswordEntry globalPasswordEntry : globalPasswordEntries) {
                envVars.put(globalPasswordEntry.getName(),
                        globalPasswordEntry.getValue().getEncryptedValue());
            }
        }
    }

    private void maskJobPasswords(Map<String, String> envVars, EnvInjectPasswordEntry[] passwordEntries) {
        for (EnvInjectPasswordEntry passwordEntry : passwordEntries) {
            envVars.put(passwordEntry.getName(), passwordEntry.getValue().getEncryptedValue());
        }
    }


}
