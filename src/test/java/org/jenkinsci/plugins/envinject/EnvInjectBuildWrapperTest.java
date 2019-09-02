package org.jenkinsci.plugins.envinject;

import static com.google.common.collect.ImmutableMap.of;
import static hudson.Util.replaceMacro;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.map;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.withEnvInjectAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.queue.QueueTaskFuture;

import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.envinject.util.TestUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import static org.junit.Assert.assertThat;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

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
                "vars.properties", "TEXT_VAR=tvalue", null, null, false, null
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        FreeStyleBuild run = j.buildAndAssertSuccess(p);

        assertEquals("tvalue", capture.getEnvVars().get("TEXT_VAR"));
        assertEquals("fvalue", capture.getEnvVars().get("FILE_VAR"));
    }

    @Test
    public void injectTextPropsFileReferenceInPropsContent() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "BASE_PATH=/tmp"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "PATH_FOO=$BASE_PATH/foo \n PATH_BAR=$BASE_PATH/bar", null, null, false, null
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp", capture.getEnvVars().get("BASE_PATH"));
        assertEquals("/tmp/foo", capture.getEnvVars().get("PATH_FOO"));
        assertEquals("/tmp/bar", capture.getEnvVars().get("PATH_BAR"));
    }

    @Test
    public void injectTextPropsFileReferenceAndCrossReferenceInPropsContent() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "INIT_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "NEW_PATH=/tmp/bar:$OLD_PATH \n OLD_PATH=$INIT_PATH", null, null, false, null
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/foo", capture.getEnvVars().get("INIT_PATH"));
        assertEquals("/tmp/foo", capture.getEnvVars().get("OLD_PATH"));
        assertEquals("/tmp/bar:/tmp/foo", capture.getEnvVars().get("NEW_PATH"));
    }

    @Test
    public void injectTextPropsContentSelfReferenceWithInitialValueFromPropsFile() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "MY_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "MY_PATH=/tmp/bar:$MY_PATH", null, null, false, null
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/bar:/tmp/foo", capture.getEnvVars().get("MY_PATH"));
    }

    @Test
    public void injectTextPropsContentOverwritesPropsFile() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "MY_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "MY_PATH=/tmp/bar", null, null, false, null
        ));

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/bar", capture.getEnvVars().get("MY_PATH"));
    }

    @Test
    public void injectTextExtendSysEnvVar() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        String oldPath = capture.getEnvVars().get("PATH");
        assertNotNull(oldPath);

        p.setScm(new SingleFileSCM("vars.properties", "PATH=ABC:$PATH"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", null, null, null, false, null
        ));
        p.scheduleBuild2(0).get();

        String newPathFromPropsFile = capture.getEnvVars().get("PATH");
        assertEquals("ABC:" + oldPath, newPathFromPropsFile);

        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                "vars.properties", "PATH=${PATH}:CBA", null, null, false, null
        ));
        p.scheduleBuild2(0).get();

        String newPathFromPropsContent = capture.getEnvVars().get("PATH");
        // NOTE: prefix ABC: from propsFile is lost
        assertEquals(oldPath + ":CBA", newPathFromPropsContent);
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
        j.assertLogContains("No such property: FOOVAR", build);
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
                        false,
                        new SecureGroovyScript(groovyScriptContent, false, null)));
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
        assertEquals(groovyScriptContent, info.getSecureGroovyScript().getScript());
        assertFalse("loadFilesFromMaster should be false", info.isLoadFilesFromMaster());
    }
    
    @Test
    @Issue("SECURITY-256")
    public void testGroovyScriptInBuildWrapper() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CREATE, Item.CONFIGURE).everywhere().to("bob");
        j.jenkins.setAuthorizationStrategy(auth);

        FreeStyleProject project = j.createFreeStyleProject();
        String script = "return [IT_IS_GROOVY: \"Indeed\"]";
        EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, false,
                                                                     new SecureGroovyScript(script, false, null));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(info);
        project.getBuildWrappersList().add(wrapper);

        //The script is not approved so should fail
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);
        j.assertBuildStatus(Result.FAILURE, future);
        //Now let bob configure the build, it should also fail
        TestUtils.saveConfigurationAs(j, project, "bob");

        future = project.scheduleBuild2(0);
        FreeStyleBuild run = j.assertBuildStatus(Result.FAILURE, future);
        //Check that it failed for the correct reason
        j.assertLogContains(UnapprovedUsageException.class.getName(), run);

        //Now let alice approve the script
        ScriptApproval.get().preapproveAll();

        //Then the build should succeed
        j.buildAndAssertSuccess(project);
    }

    private EnvInjectJobPropertyInfo withPropContent(String propertiesContent) {
        return new EnvInjectJobPropertyInfo(null, propertiesContent, null, null, false, null);
    }

}
