package org.jenkinsci.plugins.envinject.matchers;

import hudson.model.FreeStyleBuild;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

/**
 * User: lanwen
 * Date: 14.04.15
 * Time: 21:56
 */
public final class WithEnvInjectActionMatchers {
    private WithEnvInjectActionMatchers() {
        // no instances
    }

    /**
     * Gets the EnvInjectAction from build and wraps it with matcher
     * @param matcher - matcher to match EnvInjectAction
     * @return matcher to match FreeStyleBuild
     */
    public static Matcher<FreeStyleBuild> withEnvInjectAction(Matcher<EnvInjectAction> matcher) {
        return new FeatureMatcher<FreeStyleBuild, EnvInjectAction>(matcher, "envInject action", "envInject action") {
            @Override
            protected EnvInjectAction featureValueOf(FreeStyleBuild build) {
                return build.getAction(EnvInjectAction.class);
            }
        };
    }

    /**
     * Gets the env map from EnvInjectAction and wrap it with map-matcher
     * @param matcher - matcher for Map of String, String
     * @return matcher to match EnvInjectAction
     */
    public static Matcher<EnvInjectAction> map(Matcher<Map<? extends String, ? extends String>> matcher) {
        return new FeatureMatcher<EnvInjectAction, Map<? extends String, ? extends String>>(matcher, "env", "") {
            @Override
            protected Map<? extends String, ? extends String> featureValueOf(EnvInjectAction actual) {
                return actual.getEnvMap();
            }
        };
    }

    /**
     * Simple version of {@link #map(Matcher)}
     * @param key - key to find in env map of EnvInjectAction causes
     * @param value - value to find in env map of EnvInjectAction causes
     * @return matcher to match FreeStyleBuild
     */
    public static Matcher<FreeStyleBuild> withCause(String key, String value) {
        return withEnvInjectAction(map(hasEntry(key, value)));
    }

    /**
     * Same as {@link #withCause(String, String)} but with value "true"
     * @param keys - vararg of keys to find
     * @return matcher to match FreeStyleBuild
     */
    @SuppressWarnings("unchecked")
    public static Matcher<FreeStyleBuild> withCausesIsTrue(String... keys) {
        List<Matcher> causes = new ArrayList<Matcher>();
        for (String key : keys) {
            causes.add(withCause(key, "true"));
        }
        return allOf((List)causes);
    }

}
