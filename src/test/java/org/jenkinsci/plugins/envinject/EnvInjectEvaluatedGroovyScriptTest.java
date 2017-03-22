package org.jenkinsci.plugins.envinject;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import hudson.cli.CLICommand;
import hudson.cli.CLICommandInvoker;
import hudson.cli.UpdateJobCommand;
import hudson.model.*;

import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import org.apache.tools.ant.filters.StringInputStream;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.Url;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.StringContains.containsString;
import org.jenkinsci.plugins.envinject.util.TestUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


/**
 * @author Gregory Boissinot
 */
public class EnvInjectEvaluatedGroovyScriptTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testMapGroovyScript() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("jobTest");
        hudson.EnvVars.masterEnvVars.remove("JOB_NAME");

        StringBuilder groovyScriptContent = new StringBuilder();
        groovyScriptContent.append(
                "            if (CASE==null){\n" +
                        "            return null; \n" +
                        "            } \n" +
                        "\n" +
                        "            def stringValue=\"StRinG\"; \n" +
                        "\n" +
                        "            if (\"upper\".equals(CASE)){ \n" +
                        "            def map = [COMPUTE_VAR: stringValue.toUpperCase()]\n" +
                        "            return map \n" +
                        "            } \n" +
                        "            \n" +
                        "            if (\"lower\".equals(CASE)){ \n" +
                        "            def map = [COMPUTE_VAR: stringValue.toLowerCase()] \n" +
                        "            return map \n" +
                        "            } ");
        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, null, null, null, groovyScriptContent.toString(), false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty(jobPropertyInfo);
        envInjectJobProperty.setKeepJenkinsSystemVariables(true);
        envInjectJobProperty.setKeepBuildVariables(true);
        envInjectJobProperty.setOn(true);
        project.addProperty(envInjectJobProperty);

        List<ParameterValue> parameterValueList = new ArrayList<ParameterValue>();
        parameterValueList.add(new StringParameterValue("CASE", "lower"));
        ParametersAction parametersAction = new ParametersAction(parameterValueList);

        @SuppressWarnings("deprecation")
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        assertEquals(Result.SUCCESS, build.getResult());

        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        assertNotNull(envVars);

        String resultValEnvVar = envVars.get("COMPUTE_VAR");
        assertNotNull(resultValEnvVar);
        assertEquals("string", resultValEnvVar);
    }

    @Test
    public void testBuildInVars() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("jobTest");
        hudson.EnvVars.masterEnvVars.remove("JOB_NAME");
        hudson.EnvVars.masterEnvVars.remove("BUILD_NUMBER");

        StringBuilder groovyScriptContent = new StringBuilder();
        groovyScriptContent.append(
                "def env = currentJob.getLastBuild().getEnvironment()\n" +
                        "def buildNumber1 = env['BUILD_NUMBER']\n" +
                        "def buildNumber2 = currentBuild.getNumber()\n" +
                        "def map = [COMPUTE_VAR1: buildNumber1, COMPUTE_VAR2: buildNumber2]\n" +
                        "assert currentListener instanceof hudson.model.TaskListener;\n" +
                        "return map");

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, null, null, null, groovyScriptContent.toString(), false);
        EnvInjectJobProperty envInjectJobProperty = new EnvInjectJobProperty(jobPropertyInfo);
        envInjectJobProperty.setKeepJenkinsSystemVariables(true);
        envInjectJobProperty.setKeepBuildVariables(true);
        envInjectJobProperty.setOn(true);
        project.addProperty(envInjectJobProperty);

        List<ParameterValue> parameterValueList = new ArrayList<ParameterValue>();
        parameterValueList.add(new StringParameterValue("CASE", "lower"));
        ParametersAction parametersAction = new ParametersAction(parameterValueList);


        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        
        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        assertNotNull(envVars);

        String resultValEnvVar1 = envVars.get("COMPUTE_VAR1");
        String resultValEnvVar2 = envVars.get("COMPUTE_VAR2");
        assertNotNull(resultValEnvVar1);
        assertNotNull(resultValEnvVar2);
        assertEquals(resultValEnvVar2, resultValEnvVar1);
    }

    @Test
    @Issue("SECURITY-256")
    public void testGroovyScriptInJobPropertyUnderSecureJenkins() throws Exception {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CREATE, Item.CONFIGURE).everywhere().to("bob");
        jenkins.jenkins.setAuthorizationStrategy(auth);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        String script = "return [IT_IS_GROOVY: \"Indeed\"]";
        EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, false,
                                                                     new SecureGroovyScript(script, false, null));
        EnvInjectJobProperty property = new EnvInjectJobProperty(info);
        property.setKeepJenkinsSystemVariables(true);
        property.setKeepBuildVariables(true);
        property.setOn(true);
        project.addProperty(property);

        //The script is not approved so should fail
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);
        jenkins.assertBuildStatus(Result.FAILURE, future);
        //Now let bob configure the build, it should also fail
        TestUtils.saveConfigurationAs(jenkins, project, "bob");

        future = project.scheduleBuild2(0);
        FreeStyleBuild run = jenkins.assertBuildStatus(Result.FAILURE, future);
        //Check that it failed for the correct reason
        jenkins.assertLogContains("org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException", run);

        //Now let alice approve the script
        ScriptApproval.get().preapproveAll();

        //Then the build should succeed
        jenkins.buildAndAssertSuccess(project);
    }

    @Test
    @Issue("SECURITY-256")
    public void testWorkaroundSecurity86() throws Exception {
        jenkins.jenkins.setCrumbIssuer(null);
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER).everywhere().to("alice")
                .grant(Jenkins.READ, Item.READ, Item.CREATE, Item.DISCOVER, Item.CONFIGURE).everywhere().to("user");
        jenkins.jenkins.setAuthorizationStrategy(auth);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        String script = "return [IT_IS_GROOVY: \"Indeed\"]";
        EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, false,
                                                                     new SecureGroovyScript(script, false, null));
        EnvInjectJobProperty property = new EnvInjectJobProperty(info);
        property.setKeepJenkinsSystemVariables(true);
        property.setKeepBuildVariables(true);
        property.setOn(true);
        job.addProperty(property);

        TestUtils.saveConfigurationAs(jenkins, job, "alice");
        //Since alice is an admin the script should be approved automagically
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(job);

        org.jenkinsci.lib.envinject.EnvInjectAction envInjectAction = build.getAction(org.jenkinsci.lib.envinject.EnvInjectAction.class);
        assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        assertThat(envVars, hasEntry("IT_IS_GROOVY", "Indeed"));

        final String originalXml = job.getConfigFile().asString();

        job = jenkins.jenkins.getItemByFullName(job.getFullName(), FreeStyleProject.class);
        property = job.getProperty(EnvInjectJobProperty.class);
        property.setInfo(new EnvInjectJobPropertyInfo(null, null, null, null, false, new SecureGroovyScript(
                "echo \"1337 h4x0r\"\n" + script, false, null
        )));
        TestUtils.saveConfigurationAs(jenkins, job, "user");
        //Should now fail
        QueueTaskFuture<FreeStyleBuild> future = job.scheduleBuild2(0);
        jenkins.assertBuildStatus(Result.FAILURE, future);

        //Reset
        job.updateByXml((Source)new StreamSource(new StringInputStream(originalXml)));
        //Should work again
        jenkins.buildAndAssertSuccess(job);

        String hackXml = originalXml.replace("return [IT_IS_GROOVY: &quot;Indeed&quot;]", "echo &quot;m0r3 1337 h4x0r&quot;\n");
        assertThat(hackXml, containsString("m0r3 1337 h4x0r")); //Just to test myself
        JenkinsRule.WebClient wc = jenkins.createWebClient().login("user");
        WebRequest request = new WebRequest(new URL(job.getAbsoluteUrl() + "config.xml"), HttpMethod.POST);
        request.setRequestBody(hackXml);
        //wc.addCrumb(request); can't set body and crumb in the same request
        wc.getPage(request);
        //Verify it took effect
        job = jenkins.jenkins.getItemByFullName(job.getFullName(), FreeStyleProject.class);
        property = job.getProperty(EnvInjectJobProperty.class);
        assertThat(property.getInfo().getSecureGroovyScript().getScript(), containsString("m0r3 1337 h4x0r"));

        //should also fail
        future = job.scheduleBuild2(0);
        build = jenkins.assertBuildStatus(Result.FAILURE, future);
        //Check that it failed for the correct reason
        jenkins.assertLogContains("org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException", build);
    }
    
    private void approveScript(SecureGroovyScript script) {
        
    }
}
