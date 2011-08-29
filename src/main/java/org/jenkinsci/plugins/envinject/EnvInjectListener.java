package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.envinject.service.EnvInjectMasterEnvVarsSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectScriptExecutorService;
import org.jenkinsci.plugins.envinject.service.PropertiesVariablesRetriever;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectListener extends RunListener<Run> implements Serializable {

    private static Logger LOG = Logger.getLogger(EnvInjectListener.class.getName());

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        if (isEnvInjectJobPropertyActive(build)) {

            Map<String, String> resultVariables = new HashMap<String, String>();
            try {

                EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(build);
                assert envInjectJobProperty != null;
                EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
                assert envInjectJobProperty != null && envInjectJobProperty.isOn();

                //Add system environment variables if needed
                if (envInjectJobProperty.isKeepSystemVariables()) {
                    //The new envMap wins
                    resultVariables.putAll(build.getEnvironment(new LogTaskListener(LOG, Level.ALL)));
                }

                //Add build variables (such as parameter variables).
                if (envInjectJobProperty.isKeepBuildVariables()) {
                    resultVariables.putAll(getAndAddBuildVariables(build));
                }

                //Build a properties object with all information
                final Map<String, String> envMap = getEnvVarsFromInfoObject(info, resultVariables, launcher, listener);
                resultVariables.putAll(envMap);

                //Resolves vars each other
                EnvVars.resolve(resultVariables);

                //Set the new computer variables
                Computer.currentComputer().getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(resultVariables)));

                //Add a display action
                build.addAction(new EnvInjectAction(resultVariables));

            } catch (EnvInjectException envEx) {
                listener.getLogger().println("SEVERE ERROR occurs: " + envEx.getMessage());
                throw new Run.RunnerAbortedException();
            } catch (Throwable throwable) {
                listener.getLogger().println("SEVERE ERROR occurs: " + throwable.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }
        return new Environment() {
        };
    }

    private Map<String, String> getEnvVarsFromInfoObject(final EnvInjectJobPropertyInfo info, final Map<String, String> currentEnvVars, final Launcher launcher, BuildListener listener) throws Throwable {

        final Map<String, String> resultMap = new HashMap<String, String>();

        EnvInjectLogger logger = new EnvInjectLogger(listener);
        Computer computer = Computer.currentComputer();
        Node node = computer.getNode();
        if (node != null) {
            final FilePath rootPath = node.getRootPath();
            if (rootPath != null) {

                //Get env vars from properties
                resultMap.putAll(rootPath.act(new PropertiesVariablesRetriever(info, currentEnvVars, logger)));

                //Execute script info
                EnvInjectScriptExecutorService scriptExecutorService = new EnvInjectScriptExecutorService(info, currentEnvVars, rootPath, launcher, logger);
                scriptExecutorService.executeScriptFromInfoObject();
            }
        }
        return resultMap;
    }

    private Map<String, String> getAndAddBuildVariables(AbstractBuild build) {
        Map<String, String> result = new HashMap<String, String>();

        //Add build process variables
        result.putAll(build.getCharacteristicEnvVars());

        //Add build variables such as parameters, plugins contributions, ...
        result.putAll(build.getBuildVariables());

        //Add workspace variable
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            result.put("WORKSPACE", ws.getRemote());
        }
        return result;
    }

    @Override
    public void onCompleted(final Run run, final TaskListener listener) {

        if (isEnvInjectPluginActive(run)) {
            try {
                Computer.currentComputer().getNode().getRootPath().act(new EnvInjectMasterEnvVarsSetter(new EnvVars(System.getenv())));
            } catch (EnvInjectException e) {
                run.setResult(Result.FAILURE);
            } catch (InterruptedException e) {
                run.setResult(Result.FAILURE);
            } catch (IOException e) {
                run.setResult(Result.FAILURE);
            }
        }
    }


    private boolean isEnvInjectPluginActive(Run run) {

        boolean isJobPropertyChecked = isEnvInjectJobPropertyActive(run);
        if (isJobPropertyChecked) {
            return true;
        }

        Job job = run.getParent();
        if (job instanceof Project) {
            Project project = (Project) job;
            return isEnvInjectBuildWrapperActive(project.getBuildWrappersList()) || isEnvInjectBuildStepActive(project.getBuildersList());
        }

        if (job instanceof hudson.maven.MavenModuleSet) {
            hudson.maven.MavenModuleSet moduleSet = (hudson.maven.MavenModuleSet) job;
            return isEnvInjectBuildWrapperActive(moduleSet.getBuildWrappersList());
        }

        return false;
    }


    private boolean isEnvInjectJobPropertyActive(Run run) {
        EnvInjectJobProperty envInjectJobProperty = getEnvInjectJobProperty(run);
        if (envInjectJobProperty != null) {
            EnvInjectJobPropertyInfo info = envInjectJobProperty.getInfo();
            if (info != null && envInjectJobProperty.isOn()) {
                return true;
            }
        }
        return false;
    }

    private EnvInjectJobProperty getEnvInjectJobProperty(Run run) {
        return (EnvInjectJobProperty) run.getParent().getProperty(EnvInjectJobProperty.class);
    }

    private boolean isEnvInjectBuildWrapperActive(DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrapperList) {
        assert wrapperList != null;
        for (Iterator<BuildWrapper> it = wrapperList.iterator(); it.hasNext();) {
            if (EnvInjectBuildWrapper.class.isAssignableFrom(it.next().getClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnvInjectBuildStepActive(DescribableList<Builder, Descriptor<Builder>> builderList) {
        assert builderList != null;
        for (Iterator<Builder> it = builderList.iterator(); it.hasNext();) {
            if (EnvInjectBuilder.class.isAssignableFrom(it.next().getClass())) {
                return true;
            }
        }
        return false;
    }

}
