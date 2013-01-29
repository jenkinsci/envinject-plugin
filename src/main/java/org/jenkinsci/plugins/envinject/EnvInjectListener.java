package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributor;
import org.jenkinsci.plugins.envinject.service.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        if (isEligibleJobType(build)) {
            EnvInjectLogger logger = new EnvInjectLogger(listener);
            try {

                //Process environment variables at node level
                Node buildNode = build.getBuiltOn();
                if (buildNode != null) {
                    loadEnvironmentVariablesNode(build, buildNode, logger);
                }

                //Load job envinject job property
                if (isEnvInjectJobPropertyActive(build)) {
                    return setUpEnvironmentJobPropertyObject(build, launcher, listener, logger);
                } else {
                    return setUpEnvironmentWithoutJobPropertyObject(build, launcher, listener);
                }

            } catch (Run.RunnerAbortedException rre) {
                logger.info("Fail the build.");
                throw new Run.RunnerAbortedException();
            } catch (Throwable throwable) {
                logger.error("SEVERE ERROR occurs: " + throwable.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }

        return new Environment() {
        };
    }

    private boolean isEligibleJobType(AbstractBuild build) {
        if (build == null) {
            throw new IllegalArgumentException("A build object must be set.");
        }

        Job job;
        if (build instanceof MatrixRun) {
            job = ((MatrixRun) build).getParentBuild().getParent();
        } else {
            job = build.getParent();
        }

        return job instanceof BuildableItemWithBuildWrappers;

    }

    private void loadEnvironmentVariablesNode(AbstractBuild build, Node buildNode, EnvInjectLogger logger) throws EnvInjectException {

        EnvironmentVariablesNodeLoader environmentVariablesNodeLoader = new EnvironmentVariablesNodeLoader();
        Map<String, String> configNodeEnvVars = environmentVariablesNodeLoader.gatherEnvironmentVariablesNode(build, buildNode, logger);
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(buildNode.getRootPath());
        try {
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build, configNodeEnvVars);

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
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
                    if (previousEnvVars.get("WORKSPACE") == null) {
                        previousEnvVars.put("WORKSPACE", ws.getRemote());
                    }

                    //Resolve variables each other and with WORKSPACE
                    EnvInjectEnvVars envInjectEnvVars = new EnvInjectEnvVars(envInjectLogger);
                    envInjectEnvVars.resolveVars(previousEnvVars, previousEnvVars);

                    //Remove unused variables
                    Map<String, String> cleanVariables = envInjectEnvVars.removeUnsetVars(previousEnvVars);

                    //Set new env vars
                    new EnvInjectActionSetter(build.getBuiltOn().getRootPath()).addEnvVarsToEnvInjectBuildAction(build, cleanVariables);

                } catch (EnvInjectException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new Environment() {
            };
        }
    }

    private Environment setUpEnvironmentJobPropertyObject(AbstractBuild build, Launcher launcher, BuildListener listener, EnvInjectLogger logger) throws IOException, InterruptedException, EnvInjectException {

        logger.info("Preparing an environment for the build.");

        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        EnvInjectJobProperty envInjectJobProperty = variableGetter.getEnvInjectJobProperty(build);
        assert envInjectJobProperty != null;
        EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
        assert envInjectJobProperty.isOn();

        //Init infra env vars
        Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);
        Map<String, String> infraEnvVarsNode = new LinkedHashMap<String, String>(previousEnvVars);
        Map<String, String> infraEnvVarsMaster = new LinkedHashMap<String, String>(previousEnvVars);

        //Add Jenkins System variables
        if (envInjectJobProperty.isKeepJenkinsSystemVariables()) {
            logger.info("Keeping Jenkins system variables.");
            infraEnvVarsMaster.putAll(variableGetter.getJenkinsSystemVariables(true));
            infraEnvVarsNode.putAll(variableGetter.getJenkinsSystemVariables(false));
        }

        //Add build variables
        if (envInjectJobProperty.isKeepBuildVariables()) {
            logger.info("Keeping Jenkins build variables.");
            Map<String, String> buildVariables = variableGetter.getBuildVariables(build, logger);
            infraEnvVarsMaster.putAll(buildVariables);
            infraEnvVarsNode.putAll(buildVariables);
        }

        final FilePath rootPath = getNodeRootPath();
        if (rootPath != null) {

            final EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

            //Execute script
            int resultCode = envInjectEnvVarsService.executeScript(info.isLoadFilesFromMaster(),
                    info.getScriptContent(),
                    rootPath, info.getScriptFilePath(), infraEnvVarsMaster, infraEnvVarsNode, launcher, listener);
            if (resultCode != 0) {
                build.setResult(Result.FAILURE);
                throw new Run.RunnerAbortedException();
            }

            //Evaluate Groovy script
            Map<String, String> groovyMapEnvVars = envInjectEnvVarsService.executeAndGetMapGroovyScript(info.getGroovyScriptContent(), infraEnvVarsNode);

            final Map<String, String> propertiesVariables = envInjectEnvVarsService.getEnvVarsPropertiesJobProperty(rootPath,
                    logger, info.isLoadFilesFromMaster(),
                    info.getPropertiesFilePath(), info.getPropertiesContentMap(previousEnvVars),
                    infraEnvVarsMaster, infraEnvVarsNode);

            //Get variables get by contribution
            Map<String, String> contributionVariables = getEnvVarsByContribution(build, envInjectJobProperty, logger, listener);

            final Map<String, String> mergedVariables = envInjectEnvVarsService.getMergedVariables(
                    infraEnvVarsNode,
                    propertiesVariables,
                    groovyMapEnvVars,
                    contributionVariables);

            //Add an action to share injected environment variables
            new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, mergedVariables);

            //Add a wrapper to intercept the preCheckout event and add specific variables such as WORKSPACE variable
            BuildWrapperService wrapperService = new BuildWrapperService();
            wrapperService.addBuildWrapper(build, new JobSetupEnvironmentWrapper());


            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    envInjectEnvVarsService.resolveVars(mergedVariables, mergedVariables); //resolve variables each other
                    //however, here preCheckout of EnvBuildWrapper is not yet performed
                    env.putAll(mergedVariables);
                }
            };
        }
        return new Environment() {
        };
    }

    private Environment setUpEnvironmentWithoutJobPropertyObject(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, EnvInjectException {

        final Map<String, String> resultVariables = new HashMap<String, String>();

        EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);
        resultVariables.putAll(previousEnvVars);

        resultVariables.putAll(variableGetter.getJenkinsSystemVariables(false));
        resultVariables.putAll(variableGetter.getBuildVariables(build, logger));

        final FilePath rootPath = getNodeRootPath();
        if (rootPath != null) {
            new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, resultVariables);
        }

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(resultVariables);
            }
        };
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

    private Map<String, String> getEnvVarsByContribution(AbstractBuild build, EnvInjectJobProperty envInjectJobProperty,
                                                         EnvInjectLogger logger, BuildListener listener) throws EnvInjectException {

        assert envInjectJobProperty != null;
        Map<String, String> contributionVariables = new HashMap<String, String>();

        EnvInjectJobPropertyContributor[] contributors = envInjectJobProperty.getContributors();
        if (contributors != null) {
            logger.info("Injecting contributions.");
            for (EnvInjectJobPropertyContributor contributor : contributors) {
                contributionVariables.putAll(contributor.getEnvVars(build, listener));
            }
        }
        return contributionVariables;
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {

        if (!(run instanceof AbstractBuild)) {
            return;
        }

        AbstractBuild build = (AbstractBuild) run;
        if (!isEligibleJobType(build)) {
            return;
        }

        //Mask passwords
        EnvVars envVars = new EnvVars();
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        EnvInjectPasswordsMasker passwordsMasker = new EnvInjectPasswordsMasker();
        passwordsMasker.maskPasswordsIfAny(build, logger, envVars);

        if (!(build instanceof MatrixBuild)) {

            EnvInjectPluginAction envInjectAction = run.getAction(EnvInjectPluginAction.class);
            if (envInjectAction != null) {

                //Remove technical wrappers
                try {
                    BuildWrapperService wrapperService = new BuildWrapperService();
                    wrapperService.removeBuildWrappers(build, JobSetupEnvironmentWrapper.class);
                } catch (EnvInjectException e) {
                    logger.error("SEVERE ERROR occurs: " + e.getMessage());
                    throw new Run.RunnerAbortedException();
                }
            } else {
                try {
                    envVars.putAll(build.getEnvironment(listener));
                } catch (IOException e) {
                    logger.error("SEVERE ERROR occurs: " + e.getMessage());
                    throw new Run.RunnerAbortedException();
                } catch (InterruptedException e) {
                    logger.error("SEVERE ERROR occurs: " + e.getMessage());
                    throw new Run.RunnerAbortedException();
                }
            }
        }

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

}
