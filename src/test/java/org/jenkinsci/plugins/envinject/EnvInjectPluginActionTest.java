package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnvInjectPluginAction}.
 *
 * @author Oleg Nenashev
 */
@WithJenkins
class EnvInjectPluginActionTest {

    private JenkinsRule j;

    private FreeStyleProject p;
    private CaptureEnvironmentBuilder capture;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        p = j.createFreeStyleProject();

        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(null, "FOO=BAR", null, null, false, null));

        capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
    }

    @Test
    void actionSanityTest() throws Exception {
        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertEquals("BAR", capture.getEnvVars().get("FOO"));
        assertEquals("BAR", build.getEnvironment(TaskListener.NULL).get("FOO"));

        // Check action
        EnvInjectPluginAction action = build.getAction(EnvInjectPluginAction.class);
        assertNotNull(action, "EnvInjectPluginAction has not been injected");
        Object target = action.getTarget();
        assertInstanceOf(EnvInjectVarList.class, target);
        EnvInjectVarList vars = (EnvInjectVarList) target;
        assertEquals("BAR", vars.getEnvMap().get("FOO"));

        // Check that action is visible
        assertNotNull(action.getIconFileName());
        assertNotNull(action.getUrlName());
    }

    @Test
    void hideActionGlobally() throws Exception {
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();
        EnvInjectPluginConfiguration.configure(true, false, false);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertEquals("BAR", capture.getEnvVars().get("FOO"));
        assertEquals("BAR", build.getEnvironment(TaskListener.NULL).get("FOO"));

        // Check action
        EnvInjectPluginAction action = build.getAction(EnvInjectPluginAction.class);
        assertNotNull(action, "EnvInjectPluginAction has not been injected");
        Object target = action.getTarget();
        assertNull(action.getIconFileName());
        assertNull(action.getUrlName());

        assertEquals(EnvInjectVarList.HIDDEN, target, "Injected variables should be hidden");
        assertFalse(canViewInjectedVars(User.getUnknown(), build),
                "Users should have no permission to see injected vars");
    }

    @Test
    void hideActionViaPermissions() throws Exception {
        // Enable permissions
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();
        EnvInjectPluginConfiguration.configure(false, true, false);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        // Create a test user
        User user = User.get("testUser", true, null);

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy().
                grant(Jenkins.READ, Item.READ, Item.DISCOVER).everywhere().to(user.getId());
        j.jenkins.setAuthorizationStrategy(strategy);

        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertFalse(hasPermission(user, build, EnvInjectPlugin.VIEW_INJECTED_VARS),
                "User should have no View Injected Vars permission");
        assertFalse(canViewInjectedVars(user, build),
                "User should have no permission to see injected vars");

        // Grant permissions and check the results
        strategy.grant(EnvInjectPlugin.VIEW_INJECTED_VARS).everywhere().to(user.getId());
        assertTrue(hasPermission(user, build, EnvInjectPlugin.VIEW_INJECTED_VARS),
                "User should have the View Injected Vars permission");
        assertTrue(canViewInjectedVars(user, build),
                "User should have a permission to see injected vars");
    }


    private boolean hasPermission(User user, AccessControlled item, Permission permission)
            throws AssertionError {
        SecurityContext initialContext = null;
        try {
            Authentication auth = user.impersonate();
            initialContext = hudson.security.ACL.impersonate(auth);
            return item.hasPermission(permission);
        } catch (UsernameNotFoundException ex) {
            throw new AssertionError("The specified user is not found", ex);
        } finally {
            if (initialContext != null) {
                SecurityContextHolder.setContext(initialContext);
            }
        }
    }

    private boolean canViewInjectedVars(@NonNull User user, @NonNull Run<?, ?> run)
            throws UsernameNotFoundException {
        SecurityContext initialContext = null;
        Authentication auth = user.impersonate();
        initialContext = hudson.security.ACL.impersonate(auth);
        try {
            return EnvInjectPlugin.canViewInjectedVars(run);
        } finally {
            SecurityContextHolder.setContext(initialContext);
        }
    }
}
