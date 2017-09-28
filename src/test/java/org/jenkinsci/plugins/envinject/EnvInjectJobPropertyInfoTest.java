package org.jenkinsci.plugins.envinject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests of {@link EnvInjectJobPropertyInfo}.
 * @author Oleg Nenashev
 */
public class EnvInjectJobPropertyInfoTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    @Issue("SECURITY-256")
    public void shouldCreateSandboxedScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "System.exit(0)", false);
        // According to the decisions, it is true by default.
        // So the migrated scripts will require a bulk approval instead of sandboxing.
        Assert.assertTrue("Groovy sandbox must be disabled by default", !info.getSecureGroovyScript().isSandbox());
    }
    
    @Test
    @Issue("SECURITY-256")
    public void shouldNotCreateSecureGroovyScriptForNullScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, null, false);
        Assert.assertNull("Groovy script must be null", info.getSecureGroovyScript());
    }
    
    @Test
    @Issue("SECURITY-256")
    public void shouldNotCreateSecureGroovyScriptForBlankScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "   ", false);
        Assert.assertNull("Groovy script must be null if the script is blank", info.getSecureGroovyScript());
    }
    
}
