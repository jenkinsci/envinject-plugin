package org.jenkinsci.plugins.envinject;

import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import io.jenkins.plugins.casc.ConfigurationAsCode;

/**
 * Tests for {@link EnvInjectPluginConfiguration}.
 * @author Oleg Nenashev
 */
public class EnvInjectPluginConfigurationTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testRoundTrip() throws Exception {
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
        assertEquals("Value of enablePermissions differs after the configure() call",
                config.isEnablePermissions(), plugin.getConfiguration().isEnablePermissions());
        assertEquals("Value of hideInjectedVars differs after the configure() call",
                config.isHideInjectedVars(), plugin.getConfiguration().isHideInjectedVars());
        assertEquals("Value of enableLoadingFromMaster differs after the configure() call",
                config.isEnableLoadingFromMaster(), plugin.getConfiguration().isEnableLoadingFromMaster());
        plugin.save();
        
        // Reload from disk
        EnvInjectPluginConfiguration reloaded = new EnvInjectPluginConfiguration();
        assertEquals("Value of enablePermissions differs after the reload", 
                config.isEnablePermissions(), reloaded.isEnablePermissions());
        assertEquals("Value of hideInjectedVars differs after the reload", 
                config.isHideInjectedVars(), reloaded.isHideInjectedVars());
        assertEquals("Value of enableLoadingFromMaster differs after the reload",
                config.isEnableLoadingFromMaster(), reloaded.isEnableLoadingFromMaster());
    }

    @Test
    public void testConfigAsCode() throws Exception {
        ConfigurationAsCode.get().configure(EnvInjectPluginConfigurationTest.class.getResource("configuration-as-code.yml").toString());

        final EnvInjectPluginConfiguration config = EnvInjectPluginConfiguration.getInstance();
        assertTrue(config.isHideInjectedVars());
        assertTrue(config.isEnablePermissions());
        assertTrue(config.isEnableLoadingFromMaster());
    }
}
