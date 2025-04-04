package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnvInjectVarList}.
 *
 * @author Oleg Nenashev
 */
@WithJenkins
class EnvInjectVarListTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("JENKINS-44263")
    void envVarsShouldBeCachedProperlyAfterReload() throws Exception {
        final FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");
        p.getBuildersList().add(new EnvInjectBuilder(null, "TEXT_VAR=tvalue"));
        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        // Check vars before the reload
        {
            EnvInjectVarList varList = getVarListOrFail(build);
            Map<String, String> envMap = varList.getEnvMap();
            assertNotNull(envMap, "EnvInject vars list is null for the run before reload");
            assertTrue(envMap.containsKey("TEXT_VAR"), "TEXT_VAR is not present in the list");
        }

        // Reload and check vars
        // build.reload() does not assign parents for RunAction2, hence we apply a workaround
        p.doReload();
        final Run<?, ?> reloadedBuild = p.getBuildByNumber(build.getNumber());
        {
            EnvInjectVarList varList = getVarListOrFail(reloadedBuild);
            assertNotNull(varList, "EnvInject varList is null for the run after the reload");

            Map<String, String> envMap = varList.getEnvMap();
            assertNotNull(envMap, "EnvInject envMap from varList is null for the run after the reload");
            assertTrue(envMap.containsKey("TEXT_VAR"), "TEXT_VAR is not present in the list");
            assertEquals("tvalue", envMap.get("TEXT_VAR"), "TEXT_VAR has wrong value");
        }
    }

    @NonNull
    private EnvInjectVarList getVarListOrFail(@NonNull Run<?, ?> run) throws AssertionError {
        EnvInjectPluginAction action = run.getAction(EnvInjectPluginAction.class);
        assertNotNull(action, "EnvInject action is not set for the run " + run);
        EnvInjectVarList list = (EnvInjectVarList) action.getTarget();
        assertNotNull(list, "Unexpected state. EnvInject Var List is null for the run " + run);
        return list;
    }
}
