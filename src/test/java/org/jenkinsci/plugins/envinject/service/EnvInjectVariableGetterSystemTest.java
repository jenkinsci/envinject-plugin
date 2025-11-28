package org.jenkinsci.plugins.envinject.service;

import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

/**
 * @author Mark Waite
 */
@WithJenkins
class EnvInjectVariableGetterSystemTest {

    @Issue("JENKINS-69795")
    @Test
    void testSystemEnvVars(JenkinsRule jenkinsRule) throws Exception {
        boolean forceOnMaster = true;
        Map<String, String> systemEnvVars = EnvInjectVariableGetter.getJenkinsSystemEnvVars(forceOnMaster);
        assertThat(systemEnvVars, hasKey("NODE_NAME"));
        assertThat("Wrong NODE_NAME value", systemEnvVars.get("NODE_NAME"), is(Jenkins.get().getSelfLabel().getName()));
        assertThat(systemEnvVars, hasKey("NODE_LABELS"));
        assertThat("Wrong NODE_LABELS value", systemEnvVars.get("NODE_LABELS"), is(Jenkins.get().getSelfLabel().getName()));
    }
}
