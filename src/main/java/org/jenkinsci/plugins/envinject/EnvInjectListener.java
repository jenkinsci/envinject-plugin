package org.jenkinsci.plugins.envinject;

import hudson.*;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.buildwrapper.EnvInjectPasswordWrapper;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributor;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectGlobalPasswordRetriever;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {


    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        try {
            if (isEnvInjectJobPropertyActive(build)) {
                if (!isMatrixRun(build)) {
                    addBuildWrapper(build, new JobSetupEnvironmentWrapper());
                    return setUpEnvironmentNonMatrixRun(build, launcher, listener);
                } else {
                    return setUpEnvironmentMatrixRun(build, listener);
                }
            }
        } catch (EnvInjectException envEx) {
            logger.error("SEVERE ERROR occurs: " + envEx.getMessage());
            throw new Run.RunnerAbortedException();
        } catch (Run.RunnerAbortedException rre) {
            logger.info("Fail the build.");
            throw new Run.RunnerAbortedException();
        } catch (Throwable throwable) {
            logger.error("SEVERE ERROR occurs: " + throwable.getMessage());
            throw new Run.RunnerAbortedException();
        }
        return new Environment() {
        };

    }

    private void addBuildWrapper(AbstractBuild build, BuildWrapper buildWrapper) throws EnvInjectException {
        try {
            if (buildWrapper != null) {
                AbstractProject abstractProject = build.getProject();
                if (abstractProject instanceof MatrixProject) {
                    MatrixProject project = (MatrixProject) abstractProject;
                    project.getBuildWrappersList().add(buildWrapper);
                } else {
                    Project project = (Project) abstractProject;
                    project.getBuildWrappersList().add(buildWrapper);
                }
            }
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }
    }

    private boolean isEnvInjectJobPropertyActive(AbstractBuild build) {
        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);
        return envInjectJobProperty != null;
    }


    public static class JobSetupEnvironmentWrapper extends BuildWrapper {

        @SuppressWarnings("unused")
        @Extension
        public static class JobSetupEnvironmentWrapperDescriptor extends BuildWrapperDescriptor {

            public JobSetupEnvironmentWrapperDescriptor() {
            }

            public JobSetupEnvironmentWrapperDescriptor(Class<? extends BuildWrapper> clazz) {
                super(JobSetupEnvironmentWrapper.class);
            }

            @Override
            public boolean isApplicable(AbstractProject<?, ?> item) {
                return false;
            }

            @Override
            public String getDisplayName() {
                return null;
            }
        }

        @Override
        public void preCheckout(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

            EnvInjectLogger envInjectLogger = new EnvInjectLogger(listener);
            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);

            assert envInjectJobProperty != null;

            if (envInjectJobProperty.isKeepBuildVariables()) {
                try {
                    //Get previous
                    Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, envInjectLogger);
                    //Add workspace
                    FilePath ws = build.getWorkspace();
                    previousEnvVars.put("WORKSPACE", ws.getRemote());
                    //Set new env vars
                    new EnvInjectActionSetter(build.getBuiltOn().getRootPath()).addEnvVarsToEnvInjectBuildAction(build, previousEnvVars);
                } catch (EnvInjectException e) {
                    throw new IOException(e);
                }
            }
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new Environment() {
            };
        }
    }

    private Environment setUpEnvironmentNonMatrixRun(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, EnvInjectException {

        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);
        assert envInjectJobProperty != null;
        EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
        assert envInjectJobProperty != null && envInjectJobProperty.isOn();

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        logger.info("Preparing an environment for the job.");

        Map<String, String> infraEnvVarsNode = new LinkedHashMap<String, String>();
        Map<String, String> infraEnvVarsMaster = new LinkedHashMap<String, String>();

        //Add Jenkins System variables
        if (envInjectJobProperty.isKeepJenkinsSystemVariables()) {
            logger.info("Keep Jenkins system variables.");
            infraEnvVarsMaster.putAll(getJenkinsSystemVariables(true));
            infraEnvVarsNode.putAll(getJenkinsSystemVariables(false));
        }

        //Add build variables
        if (envInjectJobProperty.isKeepBuildVariables()) {
            logger.info("Keep Jenkins build variables.");
            Map<String, String> buildVariables = variableGetter.getBuildVariables(build, logger);
            infraEnvVarsMaster.putAll(buildVariables);
            infraEnvVarsNode.putAll(buildVariables);
        }

        //Inject Passwords
        injectPasswords(build, envInjectJobProperty, logger);

        //Add build parameters (or override)
        Map<String, String> parametersVariables = variableGetter.overrideParametersVariablesWithSecret(build);
        infraEnvVarsNode.putAll(parametersVariables);

        final FilePath rootPath = getNodeRootPath();
        if (rootPath != null) {

            EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

            //Execute script
            int resultCode = envInjectEnvVarsService.executeScript(info.isLoadFilesFromMaster(),
                    info.getScriptContent(),
                    rootPath, info.getScriptFilePath(), infraEnvVarsMaster, infraEnvVarsNode, launcher, listener);
            if (resultCode != 0) {
                build.setResult(Result.FAILURE);
                throw new Run.RunnerAbortedException();
            }

            final Map<String, String> propertiesVariables = envInjectEnvVarsService.getEnvVarsPropertiesJobProperty(rootPath,
                    logger, info.isLoadFilesFromMaster(),
                    info.getPropertiesFilePath(), info.getPropertiesContentMap(),
                    infraEnvVarsMaster, infraEnvVarsNode);

            //Get variables get by contribution
            Map<String, String> contributionVariables = getEnvVarsByContribution(build, envInjectJobProperty, listener);

            final Map<String, String> resultVariables = envInjectEnvVarsService.getMergedVariables(infraEnvVarsNode, contributionVariables, propertiesVariables);

            //Add an action
            new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, resultVariables);

            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.putAll(resultVariables);
                }
            };
        }
        return new Environment() {
        };
    }

    private void injectPasswords(AbstractBuild build, EnvInjectJobProperty envInjectJobProperty, EnvInjectLogger logger) throws EnvInjectException {

        //--Process global passwords
        List<EnvInjectPasswordEntry> passwordList = new ArrayList<EnvInjectPasswordEntry>();
        if (envInjectJobProperty.isInjectGlobalPasswords()) {
            logger.info("Inject global passwords.");
            EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
            EnvInjectGlobalPasswordEntry[] passwordEntries = globalPasswordRetriever.getGlobalPasswords();
            if (passwordEntries != null) {
                for (EnvInjectGlobalPasswordEntry entry : passwordEntries) {
                    passwordList.add(entry);
                }
            }
        }

        //--Process job passwords
        if (envInjectJobProperty.getPasswordEntries() != null && envInjectJobProperty.getPasswordEntries().length != 0) {
            passwordList.addAll(Arrays.asList(envInjectJobProperty.getPasswordEntries()));
        }
        //--Inject passwords
        if (passwordList.size() != 0) {
            addBuildWrapper(build, new EnvInjectPasswordWrapper(passwordList));
        }

    }

    private Environment setUpEnvironmentMatrixRun(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException, EnvInjectException {
        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        logger.info("Using environment variables injected by the parent matrix job.");
        final Map<String, String> resultVariables = variableGetter.getEnvVarsPreviousSteps(build, logger);
        final FilePath rootPath = getNodeRootPath();
        if (rootPath != null) {
            //Add an action
            new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, resultVariables);
        }
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(resultVariables);
            }
        };
    }

    private boolean isMatrixRun(AbstractBuild build) {
        return build instanceof MatrixRun;
    }

    private Map<String, String> getJenkinsSystemVariables(boolean onMaster) throws IOException, InterruptedException {

        Map<String, String> result = new TreeMap<String, String>();

        Computer computer;
        if (onMaster) {
            computer = Hudson.getInstance().toComputer();
        } else {
            computer = Computer.currentComputer();
        }

        //test if there is at least one executor
        if (computer != null) {
            result = computer.getEnvironment().overrideAll(result);
            Node n = computer.getNode();
            if (n != null)
                result.put("NODE_NAME", computer.getName());
            result.put("NODE_LABELS", Util.join(n.getAssignedLabels(), " "));
        }

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            result.put("JENKINS_URL", rootUrl);
            result.put("HUDSON_URL", rootUrl); // Legacy compatibility
        }
        result.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        result.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility

        //Global properties
        for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                EnvironmentVariablesNodeProperty environmentVariablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                result.putAll(environmentVariablesNodeProperty.getEnvVars());
            }
        }

        //Node properties
        if (computer != null) {
            Node node = computer.getNode();
            if (node != null) {
                for (NodeProperty<?> nodeProperty : node.getNodeProperties()) {
                    if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        EnvironmentVariablesNodeProperty environmentVariablesNodeProperty = (EnvironmentVariablesNodeProperty) nodeProperty;
                        result.putAll(environmentVariablesNodeProperty.getEnvVars());
                    }
                }
            }
        }
        return result;
    }

    private Node getNode() {
        Computer computer = Computer.currentComputer();
        if (computer == null) {
            return null;
        }
        return computer.getNode();
    }

    private FilePath getNodeRootPath() {
        Node node = getNode();
        if (node != null) {
            return node.getRootPath();
        }
        return null;
    }

    private boolean isParameterAction(EnvironmentContributingAction a) {

        if (a instanceof ParametersAction) {
            return true;
        }

        return false;
    }

    private boolean isEnvInjectAction(EnvironmentContributingAction a) {

        if ((EnvInjectBuilder.ENVINJECT_BUILDER_ACTION_NAME).equals(a.getDisplayName())) {
            return true;
        }

        return false;
    }


    private Map<String, String> getEnvVarsByContribution(AbstractBuild build, EnvInjectJobProperty envInjectJobProperty, BuildListener listener) throws EnvInjectException {

        assert envInjectJobProperty != null;
        Map<String, String> contributionVariables = new HashMap<String, String>();

        EnvInjectJobPropertyContributor[] contributors = envInjectJobProperty.getContributors();
        if (contributors != null) {
            for (EnvInjectJobPropertyContributor contributor : contributors) {
                contributionVariables.putAll(contributor.getEnvVars(build, listener));
            }
        }
        return contributionVariables;
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {

        EnvVars envVars = new EnvVars();
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        AbstractBuild build = (AbstractBuild) run;

        EnvInjectPluginAction envInjectAction = run.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {

            //Remove technical wrappers
            try {
                removeTechnicalWrappers(build, JobSetupEnvironmentWrapper.class, EnvInjectPasswordWrapper.class);
            } catch (EnvInjectException e) {
                logger.error("SEVERE ERROR occurs: " + e.getMessage());
                throw new Run.RunnerAbortedException();
            }

        } else {
            //Keep classic injected env vars
            AbstractBuild abstractBuild = (AbstractBuild) run;
            try {
                envVars.putAll(abstractBuild.getEnvironment(listener));
            } catch (IOException e) {
                logger.error("SEVERE ERROR occurs: " + e.getMessage());
                throw new Run.RunnerAbortedException();
            } catch (InterruptedException e) {
                logger.error("SEVERE ERROR occurs: " + e.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }

        //Mask passwords
        maskPasswordsIfAny(build, logger, envVars);

        //Add or override EnvInject Action
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(getNodeRootPath());
        try {
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction((AbstractBuild<?, ?>) run, envVars);
        } catch (EnvInjectException e) {
            logger.error("SEVERE ERROR occurs: " + e.getMessage());
            throw new Run.RunnerAbortedException();
        } catch (IOException e) {
            logger.error("SEVERE ERROR occurs: " + e.getMessage());
            throw new Run.RunnerAbortedException();
        } catch (InterruptedException e) {
            logger.error("SEVERE ERROR occurs: " + e.getMessage());
            throw new Run.RunnerAbortedException();
        }
    }

    private void removeTechnicalWrappers(AbstractBuild build, Class<JobSetupEnvironmentWrapper> jobSetupEnvironmentWrapperClass, Class<EnvInjectPasswordWrapper> envInjectPasswordWrapperClass) throws EnvInjectException {

        AbstractProject abstractProject = build.getProject();
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject;
        if (abstractProject instanceof MatrixProject) {
            MatrixProject project = (MatrixProject) abstractProject;
            wrappersProject = project.getBuildWrappersList();
        } else {
            Project project = (Project) abstractProject;
            wrappersProject = project.getBuildWrappersList();
        }

        Iterator<BuildWrapper> iterator = wrappersProject.iterator();
        while (iterator.hasNext()) {
            BuildWrapper buildWrapper = iterator.next();
            if ((((jobSetupEnvironmentWrapperClass.getName()).equals(buildWrapper.getClass().getName()))
                    || ((envInjectPasswordWrapperClass.getName()).equals(buildWrapper.getClass().getName())))) {
                try {
                    wrappersProject.remove(buildWrapper);
                } catch (IOException ioe) {
                    throw new EnvInjectException(ioe);
                }
            }
        }
    }

    private void maskPasswordsIfAny(AbstractBuild build, EnvInjectLogger logger, Map<String, String> envVars) {
        try {

            //Global passwords
            EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
            EnvInjectGlobalPasswordEntry[] globalPasswordEntries = globalPasswordRetriever.getGlobalPasswords();
            if (globalPasswordEntries != null) {
                for (EnvInjectGlobalPasswordEntry globalPasswordEntry : globalPasswordEntries) {
                    envVars.put(globalPasswordEntry.getName(),
                            globalPasswordEntry.getValue().getEncryptedValue());
                }
            }

            //Job passwords
            if (isEnvInjectJobPropertyActive(build)) {
                EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
                EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);
                EnvInjectPasswordEntry[] passwordEntries = envInjectJobProperty.getPasswordEntries();
                if (passwordEntries != null) {
                    for (EnvInjectPasswordEntry passwordEntry : passwordEntries) {
                        envVars.put(passwordEntry.getName(), passwordEntry.getValue().getEncryptedValue());
                    }
                }
            }

        } catch (EnvInjectException ee) {
            logger.error("Can't mask global password :" + ee.getMessage());
        }
    }

}
