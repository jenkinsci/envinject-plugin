package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.envinject.util.TestUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static hudson.Util.replaceMacro;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.map;
import static org.jenkinsci.plugins.envinject.matchers.WithEnvInjectActionMatchers.withEnvInjectAction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class EnvInjectBuildWrapperTest {

    private JenkinsRule j;

    @TempDir
    private File tmp;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void injectText() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "FILE_VAR=fvalue"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", "TEXT_VAR=tvalue", null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        FreeStyleBuild run = j.buildAndAssertSuccess(p);

        assertEquals("tvalue", capture.getEnvVars().get("TEXT_VAR"));
        assertEquals("fvalue", capture.getEnvVars().get("FILE_VAR"));
    }

    @Test
    void injectTextPropsFileReferenceInPropsContent() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "BASE_PATH=/tmp"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", "PATH_FOO=$BASE_PATH/foo \n PATH_BAR=$BASE_PATH/bar", null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp", capture.getEnvVars().get("BASE_PATH"));
        assertEquals("/tmp/foo", capture.getEnvVars().get("PATH_FOO"));
        assertEquals("/tmp/bar", capture.getEnvVars().get("PATH_BAR"));
    }

    @Test
    void injectTextPropsFileReferenceAndCrossReferenceInPropsContent() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "INIT_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", "NEW_PATH=/tmp/bar:$OLD_PATH \n OLD_PATH=$INIT_PATH", null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/foo", capture.getEnvVars().get("INIT_PATH"));
        assertEquals("/tmp/foo", capture.getEnvVars().get("OLD_PATH"));
        assertEquals("/tmp/bar:/tmp/foo", capture.getEnvVars().get("NEW_PATH"));
    }

    @Test
    void injectTextPropsContentSelfReferenceWithInitialValueFromPropsFile() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "MY_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", "MY_PATH=/tmp/bar:$MY_PATH", null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/bar:/tmp/foo", capture.getEnvVars().get("MY_PATH"));
    }

    @Test
    void injectTextPropsContentOverwritesPropsFile() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.properties", "MY_PATH=/tmp/foo"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", "MY_PATH=/tmp/bar", null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("/tmp/bar", capture.getEnvVars().get("MY_PATH"));
    }

    @Test
    void injectTextExtendSysEnvVar() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        String oldPath = capture.getEnvVars().get("PATH");
        assertNotNull(oldPath);

        p.setScm(new SingleFileSCM("vars.properties", "PATH=ABC:$PATH"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                "vars.properties", null, null, null, false, null
        ));
        p.getBuildWrappersList().add(wrapper);
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
    void injectFromScript() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.setScm(new SingleFileSCM("vars.groovy", "return ['FILE_VAR': 'fvalue']"));
        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                null, null, null, null, "return ['GROOVY_VAR': 'gvalue']", false
        ));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        p.scheduleBuild2(0).get();

        assertEquals("gvalue", capture.getEnvVars().get("GROOVY_VAR"));
    }

    @Test
    void exceptionMessageMustBeLogged() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(null, null, null, null, "return ['GROOVY_VAR': FOOVAR]", false));
        p.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        FreeStyleBuild build = p.scheduleBuild2(0).get();

        assertEquals(Result.FAILURE, build.getResult());
        j.assertLogContains("No such property: FOOVAR", build);
    }

    @Test
    @Issue("JENKINS-36545")
    void shouldPopulateVariableWithWorkspace() throws Exception {
        final String customWorkspaceValue = newFolder(tmp, "junit").getAbsolutePath();

        FreeStyleProject project = j.createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

        EnvVars.masterEnvVars.remove("WORKSPACE"); // ensure build node don't have such var already

        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(null, null, null, null, "return ['GROOVY_VAR': WORKSPACE]", false));
        project.getBuildWrappersList().add(wrapper);

        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        project.getBuildersList().add(capture);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertEquals(Result.SUCCESS, build.getResult());
        assertEquals(customWorkspaceValue, capture.getEnvVars().get("GROOVY_VAR"));
    }

    @Test
    void shouldPopulatePropertiesContentWithCustomWorkspace() throws Exception {
        final String customWorkspaceValue = newFolder(tmp, "junit").getAbsolutePath();
        String customEnvVarName = "materialize_workspace_path";
        String customEnvVarValue = "${WORKSPACE}/materialize_workspace";

        FreeStyleProject project = j.createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

        EnvVars.masterEnvVars.remove("WORKSPACE"); // ensure build node don't have such var already

        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper(withPropContent(customEnvVarName + "=" + customEnvVarValue));
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
    void configRoundTrip() throws Exception {
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

        wrapper = (EnvInjectBuildWrapper) project.getBuildWrappers().get(wrapper.getDescriptor());
        assertNotNull(wrapper, "There should be a build wrapper");
        EnvInjectJobPropertyInfo info = wrapper.getInfo();
        assertNotNull(info, "There should be a EnvInjectJobPropertyInfo");
        assertEquals(propertiesFilePath, info.getPropertiesFilePath());
        assertEquals(propertiesContent, info.getPropertiesContent());
        assertEquals(scriptFilePath, info.getScriptFilePath());
        assertEquals(scriptContent, info.getScriptContent());
        assertEquals(groovyScriptContent, info.getSecureGroovyScript().getScript());
        assertFalse(info.isLoadFilesFromMaster(), "loadFilesFromMaster should be false");
    }

    @Test
    @Issue("SECURITY-256")
    void testGroovyScriptInBuildWrapper() throws Exception {
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

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }

}
