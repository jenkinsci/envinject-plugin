package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe storage for globally injected environment variables.
 * 
 * <p>This class maintains separate static instances on Controller and each Agent.
 * Each JVM process maintains its own injected variables, exactly mimicking the
 * original behavior of modifying EnvVars.masterEnvVars, but without using reflection.</p>
 * 
 * @since 2.2.0
 */
public class EnvInjectGlobalStorage {
    
    /**
     * Injected environment variables for THIS JVM process.
     * On Controller, this holds Controller's injected vars.
     * On each Agent, this holds that Agent's injected vars.
     */
    private static final Map<String, String> INJECTED_VARS = new ConcurrentHashMap<>();
    
    /**
     * Get injected environment variables for the current JVM process.
     * @return Unmodifiable view of injected variables
     */
    public static Map<String, String> getInjectedVars() {
        return Collections.unmodifiableMap(INJECTED_VARS);
    }
    
    /**
     * Set injected environment variables for the current JVM process.
     * This method is called via MasterToSlaveCallable on the target node.
     * 
     * @param vars New set of environment variables to inject
     */
    public static void setInjectedVars(Map<String, String> vars) {
        // Create a copy to ensure thread safety during clear+putAll
        Map<String, String> newVars = (vars != null) ? new HashMap<>(vars) : new HashMap<>();
        INJECTED_VARS.clear();
        INJECTED_VARS.putAll(newVars);
    }
    
    /**
     * Update specific values without clearing existing ones.
     * Used when only values change but keys remain the same.
     * 
     * @param vars Variables to update
     */
    public static void updateInjectedVars(Map<String, String> vars) {
        if (vars != null) {
            INJECTED_VARS.putAll(vars);
        }
    }
    
    /**
     * Merge system environment variables with injected variables.
     * This replaces reading from EnvVars.masterEnvVars.
     * 
     * @param systemVars System environment variables from EnvVars.masterEnvVars
     * @return Merged EnvVars (system + injected, with injected taking precedence)
     */
    public static EnvVars getMergedVars(Map<String, String> systemVars) {
        EnvVars result = new EnvVars();
        
        // Start with system variables
        if (systemVars != null) {
            result.putAll(systemVars);
        }
        
        // Override with injected variables (same behavior as modifying masterEnvVars)
        result.putAll(INJECTED_VARS);
        
        return result;
    }
}
