package org.jenkinsci.plugins.envinject;

import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.springframework.util.Assert;

/**
 * Tests of {@link EnvInjectJobPropertyInfo}.
 * @author Oleg Nenashev
 */
public class EnvInjectJobPropertyInfoTest {
    
    @Test
    @Issue("SECURITY-256")
    public void shouldCreateSandboxedScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "System.exit(0)", false);
        Assert.isTrue(info.getSecureGroovyScript().isSandbox(), "Groovy sandbox must be enabled by default");
    }
    
    @Test
    @Issue("SECURITY-256")
    public void shouldNotCreateSecureGroovyScriptForNullScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, null, false);
        Assert.isNull(info.getSecureGroovyScript(), "Groovy script must be null");
    }
    
    @Test
    @Issue("SECURITY-256")
    public void shouldNotCreateSecureGroovyScriptForBlankScriptWithOldAPI() throws Exception {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "   ", false);
        Assert.isNull(info.getSecureGroovyScript(), "Groovy script must be null if the script is blank");
    }
    
}
