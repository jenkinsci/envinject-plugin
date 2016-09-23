package org.jenkinsci.plugins.envinject;

import static com.google.common.collect.ImmutableMap.of;
import static hudson.Util.replaceMacro;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.map;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.withEnvInjectAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.util.IOUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import java.io.IOException;

public class EnvInjectBuildWrapperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void injectText() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "FILE_VAR=fvalue"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "TEXT_VAR=tvalue", null, null, null, false
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        FreeStyleBuild run = j.buildAndAssertSuccess(p);

        assertEquals("tvalue", capture.getEnvVars().get("TEXT_VAR"));
        assertEquals("fvalue", capture.getEnvVars().get("FILE_VAR"));
    }

    @Test
    public void injectFromScript() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.groovy", "return ['FILE_VAR': 'fvalue']"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                null, null, null, null, "return ['GROOVY_VAR': 'gvalue']", false
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("gvalue", capture.getEnvVars().get("GROOVY_VAR"));
    }

    @Test
    public void exceptionMessageMustBeLogged() throws Exception {
    	FreeStyleProject p = j.createFreeStyleProject();
    	
    	EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
    	p.getBuildWrappersList().add(wrapper);
    	wrapper.setInfo(new EnvInjectJobPropertyInfo(null, null, null, null, "return ['GROOVY_VAR': FOOVAR]", false));
    	
    	CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
    	p.getBuildersList().add(capture);
    	FreeStyleBuild build = p.scheduleBuild2(0).get();

    	assertEquals(Result.FAILURE, build.getResult());
    	String output = IOUtils.toString(build.getLogReader());
    	assertThat("Excepted error message it's not logged", output.contains("No such property: FOOVAR"));
    }

    @Test
    @Issue("JENKINS-36545")
    public void shouldPopulateVariableWithWorkspace() throws Exception {
        final String customWorkspaceValue = tmp.newFolder().getAbsolutePath();

        FreeStyleProject project = j.createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

    	EnvVars.masterEnvVars.remove("WORKSPACE"); // ensure build node don't have such var already
    	
    	EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
    	project.getBuildWrappersList().add(wrapper);
    	wrapper.setInfo(new EnvInjectJobPropertyInfo(null, null, null, null, "return ['GROOVY_VAR': WORKSPACE]", false));

    	CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
    	project.getBuildersList().add(capture);
    	FreeStyleBuild build = project.scheduleBuild2(0).get();

    	assertEquals(Result.SUCCESS, build.getResult());
    	assertEquals(customWorkspaceValue, capture.getEnvVars().get("GROOVY_VAR"));
    }

    @Test
    public void shouldPopulatePropertiesContentWithCustomWorkspace() throws Exception {
        final String customWorkspaceValue = tmp.newFolder().getAbsolutePath();
        String customEnvVarName = "materialize_workspace_path";
        String customEnvVarValue = "${WORKSPACE}/materialize_workspace";

        FreeStyleProject project = j.createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

        EnvVars.masterEnvVars.remove("WORKSPACE"); // ensure build node don't have such var already

        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper();
        envInjectBuildWrapper.setInfo(withPropContent(customEnvVarName + "=" + customEnvVarValue));
        project.getBuildWrappersList().add(envInjectBuildWrapper);

        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        //Retrieve build workspace
        String buildWorkspaceValue = build.getWorkspace().getRemote();
        assertThat("1. Should see actual ws equal to custom", buildWorkspaceValue, equalTo(customWorkspaceValue));

        //Compute value with workspace
        String expectedCustomEnvVarValue = replaceMacro(customEnvVarValue,
                of("WORKSPACE", buildWorkspaceValue));

        assertThat("2. Property should be resolved with custom ws in build", build,
                withEnvInjectAction(map(hasEntry(customEnvVarName, expectedCustomEnvVarValue))));
    }

    @Test
    public void configRoundTrip() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        final String propertiesFilePath = "filepath.properties";
        final String propertiesContent = "PROPERTIES=CONTENT";
        final String scriptFilePath = "script/file.path";
        final String scriptContent = "echo SCRIPT=CONTENT";
        final String groovyScriptContent = "return [script:\"content\"]";

        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(
                new EnvInjectJobPropertyInfo(
                        propertiesFilePath,
                        propertiesContent,
                        scriptFilePath,
                        scriptContent,
                        groovyScriptContent,
                        false));
        project.getBuildWrappersList().add(wrapper);

        project = j.configRoundtrip(project);
        project = j.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);

        wrapper = (EnvInjectBuildWrapper)project.getBuildWrappers().get(wrapper.getDescriptor());
        assertNotNull("There should be a build wrapper", wrapper);
        EnvInjectJobPropertyInfo info = wrapper.getInfo();
        assertNotNull("There should be a EnvInjectJobPropertyInfo", info);
        assertEquals(propertiesFilePath, info.getPropertiesFilePath());
        assertEquals(propertiesContent, info.getPropertiesContent());
        assertEquals(scriptFilePath, info.getScriptFilePath());
        assertEquals(scriptContent, info.getScriptContent());
        assertEquals(groovyScriptContent, info.getGroovyScriptContent());
        assertFalse("loadFilesFromMaster should be false", info.isLoadFilesFromMaster());
    }

    private EnvInjectJobPropertyInfo withPropContent(String propertiesContent) {
        return new EnvInjectJobPropertyInfo(null, propertiesContent, null, null, null, false);
    }

}
