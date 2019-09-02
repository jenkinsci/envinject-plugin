package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import java.util.Map;
import javax.annotation.Nonnull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link EnvInjectVarList}.
 * @author Oleg Nenashev
 */
public class EnvInjectVarListTest {
    
    @Rule 
    public JenkinsRule j = new JenkinsRule();

    @Test 
    @Issue("JENKINS-44263")
    public void envVarsShouldBeCachedProperlyAfterReload() throws Exception {
        final FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");
        p.getBuildersList().add(new EnvInjectBuilder(null, "TEXT_VAR=tvalue"));
        final FreeStyleBuild build = j.buildAndAssertSuccess(p);
        
        // Check vars before the reload
        {
            EnvInjectVarList varList = getVarListOrFail(build);
            Map<String, String> envMap = varList.getEnvMap();
            Assert.assertNotNull("EnvInject vars list is null for the run before reload", envMap);
            Assert.assertTrue("TEXT_VAR is not present in the list", envMap.containsKey("TEXT_VAR"));
        }
        
        // Reload and check vars
        // build.reload() does not assign parents for RunAction2, hence we apply a workaround
        p.doReload();
        final Run<?, ?> reloadedBuild = p.getBuildByNumber(build.getNumber());
        {
            EnvInjectVarList varList = getVarListOrFail(reloadedBuild);
            Map<String, String> envMap = varList.getEnvMap();
            Assert.assertNotNull("EnvInject vars list is null for the run after the reload", envMap);
            Assert.assertTrue("TEXT_VAR is not present in the list", envMap.containsKey("TEXT_VAR"));
            Assert.assertEquals("TEXT_VAR has wrong value", "tvalue", envMap.get("TEXT_VAR"));
        }
    }
    
    @Nonnull
    private EnvInjectVarList getVarListOrFail(@Nonnull Run<?, ?> run) throws AssertionError {
        EnvInjectPluginAction action = run.getAction(EnvInjectPluginAction.class);
        Assert.assertNotNull("EnvInject action is not set for the run " + run, action);
        EnvInjectVarList list = (EnvInjectVarList)action.getTarget();
        Assert.assertNotNull("Unexpected state. EnvInject Var List is nul for the run " + run, list);
        return list;
    }
}
