package org.jenkinsci.plugins.envinject.service;

import java.util.Map;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

/**
 * @author Mark Waite
 */
public class EnvInjectVariableGetterSystemTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Issue("JENKINS-69795")
    @Test
    public void testSystemEnvVars() throws Exception {
        boolean forceOnMaster = true;
        Map<String, String> systemEnvVars = EnvInjectVariableGetter.getJenkinsSystemEnvVars(forceOnMaster);
        assertThat(systemEnvVars, hasKey("NODE_NAME"));
        assertThat("Wrong NODE_NAME value", systemEnvVars.get("NODE_NAME"), is(Jenkins.get().getSelfLabel().getName()));
        assertThat(systemEnvVars, hasKey("NODE_LABELS"));
        assertThat("Wrong NODE_LABELS value", systemEnvVars.get("NODE_LABELS"), is(Jenkins.get().getSelfLabel().getName()));
    }
}
