package org.jenkinsci.plugins.envinject;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnvInjectPluginConfiguration}.
 *
 * @author Oleg Nenashev
 */
@WithJenkins
class EnvInjectPluginConfigurationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testRoundTrip() throws Exception {
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();

        // Launch roundtrips with different params
        testRoundtrip(new EnvInjectPluginConfiguration(true, false, false));
        testRoundtrip(new EnvInjectPluginConfiguration(false, true, false));
        testRoundtrip(new EnvInjectPluginConfiguration(false, false, false));
        testRoundtrip(new EnvInjectPluginConfiguration(false, false, true));
    }

    private void testRoundtrip(EnvInjectPluginConfiguration config) throws IOException {
        final EnvInjectPlugin plugin = EnvInjectPlugin.getInstance();
        EnvInjectPluginConfiguration.configure(config.isHideInjectedVars(), config.isEnablePermissions(), config.isEnableLoadingFromMaster());
        assertEquals(config.isEnablePermissions(), plugin.getConfiguration().isEnablePermissions(), "Value of enablePermissions differs after the configure() call");
        assertEquals(config.isHideInjectedVars(), plugin.getConfiguration().isHideInjectedVars(), "Value of hideInjectedVars differs after the configure() call");
        assertEquals(config.isEnableLoadingFromMaster(), plugin.getConfiguration().isEnableLoadingFromMaster(), "Value of enableLoadingFromMaster differs after the configure() call");
        plugin.save();

        // Reload from disk
        EnvInjectPluginConfiguration reloaded = new EnvInjectPluginConfiguration();
        assertEquals(config.isEnablePermissions(), reloaded.isEnablePermissions(), "Value of enablePermissions differs after the reload");
        assertEquals(config.isHideInjectedVars(), reloaded.isHideInjectedVars(), "Value of hideInjectedVars differs after the reload");
        assertEquals(config.isEnableLoadingFromMaster(), reloaded.isEnableLoadingFromMaster(), "Value of enableLoadingFromMaster differs after the reload");
    }

    @Test
    void testConfigAsCode() {
        ConfigurationAsCode.get().configure(EnvInjectPluginConfigurationTest.class.getResource("configuration-as-code.yml").toString());

        final EnvInjectPluginConfiguration config = EnvInjectPluginConfiguration.getInstance();
        assertTrue(config.isHideInjectedVars());
        assertTrue(config.isEnablePermissions());
        assertTrue(config.isEnableLoadingFromMaster());
    }
}
