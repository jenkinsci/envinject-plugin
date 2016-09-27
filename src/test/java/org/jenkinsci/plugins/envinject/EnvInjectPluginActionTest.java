package org.jenkinsci.plugins.envinject;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

/**
 * Tests for {@link EnvInjectPluginAction}.
 * @author Oleg Nenashev
 */
public class EnvInjectPluginActionTest {
        
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private FreeStyleProject p;
    private CaptureEnvironmentBuilder capture;
    
    @Before
    public void prepareProject() throws Exception {
        p = j.createFreeStyleProject();

        EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(null, "FOO=BAR", null, null, null, false));
        
        capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
    }
    
    @Test
    public void actionSanityTest() throws Exception {
        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertEquals("BAR", capture.getEnvVars().get("FOO"));
        assertEquals("BAR", build.getEnvironment(TaskListener.NULL).get("FOO"));
        
        // Check action
        EnvInjectPluginAction action = build.getAction(EnvInjectPluginAction.class);
        assertNotNull("EnvInjectPluginAction has not been injected", action);
        Object target = action.getTarget();
        assertTrue(target instanceof EnvInjectVarList);
        EnvInjectVarList vars = (EnvInjectVarList)target;
        assertEquals("BAR", vars.getEnvMap().get("FOO"));
        
        // Check that action is visible
        assertNotNull(action.getIconFileName());
        assertNotNull(action.getUrlName());
    }
    
    @Test
    public void hideActionGlobally() throws Exception {
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();
        EnvInjectPluginConfiguration.configure(true, false);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertEquals("BAR", capture.getEnvVars().get("FOO"));
        assertEquals("BAR", build.getEnvironment(TaskListener.NULL).get("FOO"));
        
        // Check action
        EnvInjectPluginAction action = build.getAction(EnvInjectPluginAction.class);
        assertNotNull("EnvInjectPluginAction has not been injected", action);
        Object target = action.getTarget();
        assertNull(action.getIconFileName());
        assertNull(action.getUrlName());
        
        assertEquals("Injected variables should be hidden", EnvInjectVarList.HIDDEN, target);
        assertFalse("Users should have no permission to see injected vars", 
                canViewInjectedVars(User.getUnknown(), build));
    }
    
    @Test
    public void hideActionViaPermissions() throws Exception {
        // Enable permissions
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();
        EnvInjectPluginConfiguration.configure(false, true);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        // Create a test user
        User user = User.get("testUser", true, null);

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy().
            grant(Jenkins.READ, Item.READ, Item.DISCOVER).everywhere().to(user.getId());
        j.jenkins.setAuthorizationStrategy(strategy);
        
        // Run build and retrieve results
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertFalse("User should have no View Injected Vars permission", 
                hasPermission(user, build, EnvInjectPlugin.VIEW_INJECTED_VARS));
        assertFalse("User should have no permission to see injected vars", 
                canViewInjectedVars(user, build));
            
        // Grant permissions and check the results
        strategy.grant(EnvInjectPlugin.VIEW_INJECTED_VARS).everywhere().to(user.getId());
        assertTrue("User should have the View Injected Vars permission", 
                hasPermission(user, build, EnvInjectPlugin.VIEW_INJECTED_VARS));
        assertTrue("User should have a permission to see injected vars", 
                canViewInjectedVars(user, build));
    }
    
    
    private boolean hasPermission(User user, AccessControlled item, Permission permission ) 
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
    
    private boolean canViewInjectedVars(@Nonnull User user, @Nonnull Run<?,?> run) 
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
