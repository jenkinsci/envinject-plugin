package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnvInjectJobProperty}
 * @author Oleg Nenashev
 */
@WithJenkins
class EnvInjectJobPropertyTest {

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void shouldInjectBuildVarsFromPropertiesContent() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "FOO=BAR");

        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        assertEquals("BAR",
                build.getEnvironment(TaskListener.NULL).get("FOO"),
                "The FOO variable has not been injected properly");
    }

    @Test
    void shouldNotInjectVariablesIfPropertyIsDisabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        EnvInjectJobProperty<FreeStyleProject> prop = new EnvInjectJobProperty<>(
                new EnvInjectJobPropertyInfo(null, "FOO=BAR", null, null, false, null));
        // prop.setOn(false); // It is default
        project.addProperty(prop);

        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        assertNull(build.getEnvironment(TaskListener.NULL).get("FOO"),
                "The plugin should not inject properties by default");
    }

    @Test
    void shouldKeepBuildVariablesByDefault() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("PARAM", "")));
        // We assign a value to another parameter just to enable the engine
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM2=Overridden");

        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "Value")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("Value",
                build.getEnvironment(TaskListener.NULL).get("PARAM"),
                "The build parameter has been overridden");
    }

    @Test
    @Disabled("The value is being actually contributed by other env contributors. The feature seems to be obsolete")
    void shouldNotKeepBuildVariablesIfDisabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM2=Overridden");
        prop.setKeepBuildVariables(false);

        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "Value")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertNull(build.getEnvironment(TaskListener.NULL).get("PARAM"),
                "We expect that the PARAM is not specified");
    }

    @Test
    @Disabled("It should not override vars according to the manual testing. But it does... "
            + "Manual tests also show the wrong value in InjectedVarsAction")
    @Issue("JENKINS-29905")
    void shouldNotOverrideBuildParametersByDefault() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM=Overridden");
        prop.setOverrideBuildParameters(false);

        final CaptureEnvironmentBuilder envCapture = new CaptureEnvironmentBuilder();
        project.getBuildersList().add(envCapture);

        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "ValueFromParameter")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("ValueFromParameter", envCapture.getEnvVars().get("PARAM"), "The variable has been overridden in the environment");
        assertEquals("ValueFromParameter", build.getEnvironment(TaskListener.NULL).get("PARAM"), "The variable has been overridden in the API");

        // Ensure that Parameters action contains the correct value
        EnvInjectPluginAction a = build.getAction(EnvInjectPluginAction.class);
        assertNotNull(a, "EnvInjectPluginAction has not been added to the build");
        EnvVars vars = new EnvVars();
        a.buildEnvVars(build, vars);
        assertEquals("ValueFromParameter", vars.get("PARAM"), "The variable has been overridden in the stored action");
    }

    @Test
    void shouldOverrideBuildParametersIfEnabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM=Overridden");
        prop.setOverrideBuildParameters(true);

        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "ValueFromParameter")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("Overridden", build.getEnvironment(TaskListener.NULL).get("PARAM"), "The build parameter value has not been overridden");

        // Ensure that Parameters action contains the correct value
        EnvInjectPluginAction a = build.getAction(EnvInjectPluginAction.class);
        assertNotNull(a, "EnvInjectPluginAction has not been added to the build");
        EnvVars vars = new EnvVars();
        a.buildEnvVars(build, vars);
        assertEquals("Overridden", vars.get("PARAM"), "The build parameter value has not been overridden in EnvInjectPluginAction");

    }

    @Test
    void configRoundTrip() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        final String propertiesFilePath = "filepath.properties";
        final String propertiesContent = "PROPERTIES=CONTENT";
        final String scriptFilePath = "script/file.path";
        final String scriptContent = "echo SCRIPT=CONTENT";
        final String groovyScriptContent = "return [script:\"content\"]";
        EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(
                propertiesFilePath,
                propertiesContent,
                scriptFilePath,
                scriptContent,
                true,
                new SecureGroovyScript(groovyScriptContent, false, null));
        EnvInjectJobProperty property = new EnvInjectJobProperty<FreeStyleProject>(info);
        property.setOn(true);
        property.setKeepBuildVariables(false);
        property.setKeepJenkinsSystemVariables(false);
        property.setOverrideBuildParameters(true);
        project.addProperty(property);

        project = jenkinsRule.configRoundtrip(project);
        project = jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);

        property = project.getProperty(EnvInjectJobProperty.class);
        assertNotNull(property, "there should be a property");
        info = property.getInfo();
        assertNotNull(info, "There should be an info object");
        assertTrue(property.isOn(), "Property should be on");
        assertFalse(property.isKeepBuildVariables(), "KeepBuildVariables");
        assertFalse(property.isKeepJenkinsSystemVariables(), "KeepJenkinsSystemVariables");
        assertTrue(property.isOverrideBuildParameters(), "OverrideBuildParameters");
        assertEquals(propertiesFilePath, info.getPropertiesFilePath());
        assertEquals(propertiesContent, info.getPropertiesContent());
        assertEquals(scriptFilePath, info.getScriptFilePath());
        assertEquals(scriptContent, info.getScriptContent());
        assertEquals(groovyScriptContent, info.getSecureGroovyScript().getScript());
        assertTrue(info.isLoadFilesFromMaster(), "loadFilesFromMaster should be true");
    }

    @NonNull
    public EnvInjectJobProperty<FreeStyleProject>
    forPropertiesContent(@NonNull FreeStyleProject job, @NonNull String content) throws IOException {
        final EnvInjectJobProperty<FreeStyleProject> prop = new EnvInjectJobProperty<>(
                new EnvInjectJobPropertyInfo(null, content, null, null, false, null));
        prop.setOn(true); // Property becomes enabled by default
        job.addProperty(prop);
        return prop;
    }
}
