package org.jenkinsci.plugins.envinject;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for {@link EnvInjectJobProperty}
 * @author Oleg Nenashev
 */
public class EnvInjectJobPropertyTest {
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void shouldInjectBuildVarsFromPropertiesContent() throws Exception {   
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "FOO=BAR");
        
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        assertEquals("The FOO variable has not been injected properly", "BAR", 
                build.getEnvironment(TaskListener.NULL).get("FOO"));
    }
    
    @Test
    public void shouldNotInjectVariablesIfPropertyIsDisabled() throws Exception {   
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        EnvInjectJobProperty<FreeStyleProject> prop = new EnvInjectJobProperty<FreeStyleProject>(
                new EnvInjectJobPropertyInfo(null, "FOO=BAR", null, null, null, false));
        // prop.setOn(false); // It is default
        project.addProperty(prop);
        
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        assertNull("The plugin should not inject properties by default", 
                build.getEnvironment(TaskListener.NULL).get("FOO"));
    }
    
    @Test
    public void shouldKeepBuildVariablesByDefault() throws Exception {   
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();     
        // We assign a value to another parameter just to enable the engine
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM2=Overridden");
        
        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "Value")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("The build parameter has been overridden", "Value",
                build.getEnvironment(TaskListener.NULL).get("PARAM"));
    }
    
    @Test
    @Ignore("The value is being actually contributed by other env contributors. The feature seems to be obsolete")
    public void shouldNotKeepBuildVariablesIfDisabled() throws Exception {   
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM2=Overridden");
        prop.setKeepBuildVariables(false);
        
        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "Value")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertNull("We expect that the PARAM is not specified", 
                build.getEnvironment(TaskListener.NULL).get("PARAM"));
    }
    
    @Test
    @Ignore("It should not override vars according to the manual testing. But it does... "
            + "Manual tests also show the wrong value in InjectedVarsAction")
    @Issue("JENKINS-29905")
    public void shouldNotOverrideBuildParametersByDefault() throws Exception {
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
        assertEquals("The variable has been overridden in the environment", "ValueFromParameter", envCapture.getEnvVars().get("PARAM"));
        assertEquals("The variable has been overridden in the API", "ValueFromParameter", build.getEnvironment(TaskListener.NULL).get("PARAM"));
    }
    
    @Test
    public void shouldOverrideBuildParametersIfEnabled() throws Exception {   
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();      
        EnvInjectJobProperty<FreeStyleProject> prop = forPropertiesContent(project, "PARAM=Overridden");
        prop.setOverrideBuildParameters(true);
        
        QueueTaskFuture<FreeStyleBuild> scheduled = project.scheduleBuild2(0, new Cause.UserIdCause(),
                new ParametersAction(new StringParameterValue("PARAM", "ValueFromParameter")));
        assertNotNull(scheduled);
        FreeStyleBuild build = scheduled.get();
        jenkinsRule.assertBuildStatusSuccess(build);
        assertEquals("The build parameter value has not been overridden", "Overridden", build.getEnvironment(TaskListener.NULL).get("PARAM"));
    }

    @Test
    public void configRoundTrip() throws Exception {
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
                groovyScriptContent,
                true);
        EnvInjectJobProperty property = new EnvInjectJobProperty<FreeStyleProject>(info);
        property.setOn(true);
        property.setKeepBuildVariables(false);
        property.setKeepJenkinsSystemVariables(false);
        property.setOverrideBuildParameters(true);
        project.addProperty(property);

        project = jenkinsRule.configRoundtrip(project);
        project = jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);

        property = project.getProperty(EnvInjectJobProperty.class);
        assertNotNull("there should be a property", property);
        info = property.getInfo();
        assertNotNull("There should be an info object", info);
        assertTrue("Property should be on", property.isOn());
        assertFalse("KeepBuildVariables", property.isKeepBuildVariables());
        assertFalse("KeepJenkinsSystemVariables", property.isKeepJenkinsSystemVariables());
        assertTrue("OverrideBuildParameters", property.isOverrideBuildParameters());
        assertEquals(propertiesFilePath, info.getPropertiesFilePath());
        assertEquals(propertiesContent, info.getPropertiesContent());
        assertEquals(scriptFilePath, info.getScriptFilePath());
        assertEquals(scriptContent, info.getScriptContent());
        assertEquals(groovyScriptContent, info.getGroovyScriptContent());
        assertTrue("loadFilesFromMaster should be true", info.isLoadFilesFromMaster());
    }
    
    @Nonnull
    public EnvInjectJobProperty<FreeStyleProject>
            forPropertiesContent(@Nonnull FreeStyleProject job, @Nonnull String content) throws IOException {
        final EnvInjectJobProperty<FreeStyleProject> prop = new EnvInjectJobProperty<FreeStyleProject>(
                new EnvInjectJobPropertyInfo(null, content, null, null, null, false));
        prop.setOn(true); // Property becomes enabled by default
        job.addProperty(prop);
        return prop;
    }   
}
