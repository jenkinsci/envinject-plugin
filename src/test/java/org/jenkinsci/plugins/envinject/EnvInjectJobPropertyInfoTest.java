package org.jenkinsci.plugins.envinject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests of {@link EnvInjectJobPropertyInfo}.
 * @author Oleg Nenashev
 */
@WithJenkins
class EnvInjectJobPropertyInfoTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("SECURITY-256")
    void shouldCreateSandboxedScriptWithOldAPI() {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "System.exit(0)", false);
        // According to the decisions, it is true by default.
        // So the migrated scripts will require a bulk approval instead of sandboxing.
        assertFalse(info.getSecureGroovyScript().isSandbox(), "Groovy sandbox must be disabled by default");
    }

    @Test
    @Issue("SECURITY-256")
    void shouldNotCreateSecureGroovyScriptForNullScriptWithOldAPI() {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, false, null);
        assertNull(info.getSecureGroovyScript(), "Groovy script must be null");
    }

    @Test
    @Issue("SECURITY-256")
    void shouldNotCreateSecureGroovyScriptForBlankScriptWithOldAPI() {
        final EnvInjectJobPropertyInfo info = new EnvInjectJobPropertyInfo(null, null, null, null, "   ", false);
        assertNull(info.getSecureGroovyScript(), "Groovy script must be null if the script is blank");
    }

}
