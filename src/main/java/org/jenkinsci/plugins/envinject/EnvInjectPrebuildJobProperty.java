package org.jenkinsci.plugins.envinject;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributor;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributorDescriptor;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectContributorManagement;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Arcadiy Ivanov (arcivanov)
 * @author Gregory Boissinot
 */
public class EnvInjectPrebuildJobProperty<T extends Job<?, ?>> extends JobProperty<T> {

    private EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo();
    private boolean on;
    private boolean keepJenkinsSystemVariables;
    private boolean keepBuildVariables;
    private boolean overrideBuildParameters;
    private EnvInjectJobPropertyContributor[] contributors;

    private transient EnvInjectJobPropertyContributor[] contributorsComputed;

    @SuppressWarnings("unused")
    public EnvInjectJobPropertyInfo getInfo() {
        return info;
    }

    @SuppressWarnings("unused")
    public boolean isOn() {
        return on;
    }

    @SuppressWarnings("unused")
    public boolean isKeepJenkinsSystemVariables() {
        return keepJenkinsSystemVariables;
    }

    @SuppressWarnings("unused")
    public boolean isKeepBuildVariables() {
        return keepBuildVariables;
    }

    @SuppressWarnings("unused")
    public boolean isOverrideBuildParameters() {
        return overrideBuildParameters;
    }

    @SuppressWarnings("unused")
    public EnvInjectJobPropertyContributor[] getContributors() {
        if (contributorsComputed == null) {
            try {
                contributorsComputed = computeEnvInjectContributors();
            } catch (org.jenkinsci.lib.envinject.EnvInjectException e) {
                e.printStackTrace();
            }
            contributors = contributorsComputed;
        }

        return Arrays.copyOf(contributors, contributors.length);
    }

    private EnvInjectJobPropertyContributor[] computeEnvInjectContributors() throws org.jenkinsci.lib.envinject.EnvInjectException {

        DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor>
                descriptors = EnvInjectJobPropertyContributor.all();

        // If the config are loaded with success (this step) and the descriptors size doesn't have change
        // we considerate, they are the same, therefore we retrieve instances
        if (contributors != null && contributors.length == descriptors.size()) {
            return contributors;
        }

        EnvInjectContributorManagement envInjectContributorManagement = new EnvInjectContributorManagement();
        EnvInjectJobPropertyContributor[] contributorsInstance = envInjectContributorManagement.getNewContributorsInstance();

        //No jobProperty Contributors ==> new configuration
        if (contributors == null || contributors.length == 0) {
            return contributorsInstance;
        }

        List<EnvInjectJobPropertyContributor> result = new ArrayList<EnvInjectJobPropertyContributor>();
        for (EnvInjectJobPropertyContributor contributor1 : contributorsInstance) {
            for (EnvInjectJobPropertyContributor contributor2 : contributors) {
                if (contributor1.getDescriptor().getClass() == contributor2.getDescriptor().getClass()) {
                    result.add(contributor2);
                } else {
                    result.add(contributor1);
                }
            }
        }
        return result.toArray(new EnvInjectJobPropertyContributor[result.size()]);
    }

    public void setInfo(EnvInjectJobPropertyInfo info) {
        this.info = info;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setKeepJenkinsSystemVariables(boolean keepJenkinsSystemVariables) {
        this.keepJenkinsSystemVariables = keepJenkinsSystemVariables;
    }

    public void setKeepBuildVariables(boolean keepBuildVariables) {
        this.keepBuildVariables = keepBuildVariables;
    }

    public void setOverrideBuildParameters(boolean overrideBuildParameters) {
        this.overrideBuildParameters = overrideBuildParameters;
    }

    public void setContributors(EnvInjectJobPropertyContributor[] jobPropertyContributors) {
        this.contributors = jobPropertyContributors;
    }

    @Override
    public JobProperty<?> reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        EnvInjectPrebuildJobProperty property = (EnvInjectPrebuildJobProperty) super.reconfigure(req, form);
        if (property != null && property.info != null && !Jenkins.getInstance().hasPermission(Jenkins.RUN_SCRIPTS)) {
            // Don't let non RUN_SCRIPT users set arbitrary groovy script
            property.info = new EnvInjectJobPropertyInfo(property.info.propertiesFilePath, property.info.propertiesContent,
                                                         property.info.getScriptFilePath(), property.info.getScriptContent(),
                                                         this.info != null ? this.info.getGroovyScriptContent() : "",
                                                         property.info.isLoadFilesFromMaster());
        }
        return property;
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "[Environment Inject (Prebuild)] -" + Messages.envinject_set_displayName();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help.html";
        }

        @Override
        public EnvInjectPrebuildJobProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            Object onObject = formData.get("on");

            if (onObject != null) {
                EnvInjectPrebuildJobProperty envInjectJobProperty = new EnvInjectPrebuildJobProperty();
                EnvInjectJobPropertyInfo info = req.bindParameters(EnvInjectJobPropertyInfo.class, "envInjectInfoPrebuildJobProperty.");
                envInjectJobProperty.setInfo(info);
                envInjectJobProperty.setOn(true);
                if (onObject instanceof JSONObject) {
                    JSONObject onJSONObject = (JSONObject) onObject;
                    envInjectJobProperty.setKeepJenkinsSystemVariables(onJSONObject.getBoolean("keepJenkinsSystemVariables"));
                    envInjectJobProperty.setKeepBuildVariables(onJSONObject.getBoolean("keepBuildVariables"));
                    envInjectJobProperty.setOverrideBuildParameters(onJSONObject.getBoolean("overrideBuildParameters"));

                    //Process contributions
                    setContributors(req, envInjectJobProperty, onJSONObject);

                    return envInjectJobProperty;
                }
            }

            return null;
        }

        private void setContributors(StaplerRequest req, EnvInjectPrebuildJobProperty envInjectJobProperty, JSONObject onJSONObject) {
            if (!onJSONObject.containsKey("contributors")) {
                envInjectJobProperty.setContributors(new EnvInjectJobPropertyContributor[0]);
            } else {
                JSON contribJSON;
                try {
                    contribJSON = onJSONObject.getJSONArray("contributors");
                } catch (JSONException jsone) {
                    contribJSON = onJSONObject.getJSONObject("contributors");
                }
                List<EnvInjectJobPropertyContributor> contributions = req.bindJSONToList(EnvInjectJobPropertyContributor.class, contribJSON);
                EnvInjectJobPropertyContributor[] contributionsArray = contributions.toArray(new EnvInjectJobPropertyContributor[contributions.size()]);
                envInjectJobProperty.setContributors(contributionsArray);
            }
        }

        public DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor> getEnvInjectContributors() {
            return EnvInjectJobPropertyContributor.all();
        }

        public @CheckForNull EnvInjectJobPropertyContributor[] getContributorsInstance() {
            EnvInjectContributorManagement envInjectContributorManagement = new EnvInjectContributorManagement();
            try {
                return envInjectContributorManagement.getNewContributorsInstance();
            } catch (org.jenkinsci.lib.envinject.EnvInjectException e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean isEnvInjectContributionActivated() {
            EnvInjectContributorManagement envInjectContributorManagement = new EnvInjectContributorManagement();
            return envInjectContributorManagement.isEnvInjectContributionActivated();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.model.JobProperty#prebuild(hudson.model.AbstractBuild,
     * hudson.model.BuildListener)
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if(!isOn()) {
            return true;
        }
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        FilePath ws = build.getWorkspace();

        try {
            logger.info("Preparing an environment for the build (prebuild phase).");

            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            EnvInjectJobPropertyInfo info = getInfo();
            assert isOn();

            // Init infra env vars
            Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);
            Map<String, String> infraEnvVarsNode = new LinkedHashMap<String, String>(previousEnvVars);
            Map<String, String> infraEnvVarsMaster = new LinkedHashMap<String, String>(previousEnvVars);

            // Add workspace if not set
            if (ws != null) {
                if (infraEnvVarsNode.get("WORKSPACE") == null) {
                    infraEnvVarsNode.put("WORKSPACE", ws.getRemote());
                }
            }

            //Add Jenkins System variables
            if (isKeepJenkinsSystemVariables()) {
                logger.info("Keeping Jenkins system variables.");
                infraEnvVarsMaster.putAll(variableGetter.getJenkinsSystemVariables(true));
                infraEnvVarsNode.putAll(variableGetter.getJenkinsSystemVariables(false));
            }

            //Add build variables
            if (isKeepBuildVariables()) {
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
                        rootPath, info.getScriptFilePath(), infraEnvVarsMaster, infraEnvVarsNode, rootPath.createLauncher(listener), listener);
                if (resultCode != 0) {
                    build.setResult(Result.FAILURE);
                    throw new Run.RunnerAbortedException();
                }

                //Evaluate Groovy script
                Map<String, String> groovyMapEnvVars = envInjectEnvVarsService.executeAndGetMapGroovyScript(logger, info.getGroovyScriptContent(), infraEnvVarsNode);

                final Map<String, String> propertiesVariables = envInjectEnvVarsService.getEnvVarsPropertiesJobProperty(rootPath,
                        logger, info.isLoadFilesFromMaster(),
                        info.getPropertiesFilePath(), info.getPropertiesContentMap(previousEnvVars),
                        infraEnvVarsMaster, infraEnvVarsNode);

                //Get variables get by contribution
                Map<String, String> contributionVariables = getEnvVarsByContribution(build, this, logger, listener);

                final Map<String, String> mergedVariables = envInjectEnvVarsService.getMergedVariables(
                        infraEnvVarsNode,
                        propertiesVariables,
                        groovyMapEnvVars,
                        contributionVariables);

                //Add an action to share injected environment variables
                new EnvInjectActionSetter(rootPath).addEnvVarsToEnvInjectBuildAction(build, mergedVariables);
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            logger.error("Failure while executing scripts in a prebuild phase: " + sw.toString());
            pw.close();
            return false;
        }
        return true;
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
    
    private Map<String, String> getEnvVarsByContribution(AbstractBuild build, EnvInjectPrebuildJobProperty envInjectJobProperty,
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

    
    @Deprecated
    private transient boolean injectGlobalPasswords;
    @Deprecated
    private transient EnvInjectPasswordEntry[] passwordEntries;
    @Deprecated
    private transient boolean keepSystemVariables;

    @Deprecated
    public boolean isInjectGlobalPasswords() {
        return injectGlobalPasswords;
    }

    @Deprecated
    public EnvInjectPasswordEntry[] getPasswordEntries() {
        return passwordEntries;
    }
}
